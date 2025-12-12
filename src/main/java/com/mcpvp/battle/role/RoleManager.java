package com.mcpvp.battle.role;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePreferences;
import com.mcpvp.battle.game.BattleGameState;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.preference.PreferenceManager;
import com.mcpvp.battle.event.PlayerJoinTeamEvent;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.event.PlayerResignEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player roles (Attack/Defense), persistence and tablist tags,
 * and exposes per-player objective text based on role + flag state.
 */
@Getter
@RequiredArgsConstructor
public class RoleManager extends EasyLifecycle implements EasyListener {

    private final Plugin plugin;
    private final Battle battle;
    private final PreferenceManager preferenceManager;

    // Current role during this session
    private final Map<UUID, Role> roles = new ConcurrentHashMap<>();

    private static final boolean AUTO_OPEN_ROLE_GUI = false;

    // Last objective text announced in chat for each player
    private final Map<UUID, String> lastObjectiveAnnouncement = new ConcurrentHashMap<>();

    public void init() {
        this.attach((EasyListener) this);
    }

    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }

    public Role getRole(Player player) {
        return this.roles.get(player.getUniqueId());
    }

    public void setRole(Player player, Role role, boolean persist) {
        UUID id = player.getUniqueId();

        if (role == null) {
            this.roles.remove(id);
        } else {
            this.roles.put(id, role);
        }

        if (persist) {
            this.preferenceManager.store(player, BattlePreferences.ROLE, role);
        }

        this.updateTabName(player);
        //this.checkAndAnnounceObjective(player);
    }

    /**
     * Loads a previously stored role for the player, if present.
     */
    public void loadRole(Player player) {
        this.preferenceManager
                .find(player, BattlePreferences.ROLE)
                .ifPresent(role -> {
                    this.roles.put(player.getUniqueId(), role);
                    this.updateTabName(player);
                });
    }

    /**
     * Updates the player's tablist name with team color + [A]/[D] tag.
     */
    public void updateTabName(Player player) {
        BattleTeam team = this.battle.getGame().getTeamManager().getTeam(player);
        String baseColor = team != null ? team.getColor().getChatString() : C.WHITE;

        Role role = this.getRole(player);
        String tag = "";

        if (role == Role.ATTACK) {
            tag = C.YELLOW + " [A]";
        } else if (role == Role.DEFENSE) {
            tag = C.AQUA + " [D]";
        }

        player.setPlayerListName(baseColor + player.getName() + C.GRAY + tag);
    }

    /**
     * Computes the plain objective text for the sidebar, without colors or "Objective:" prefix.
     *
     * Defense role:
     * - Flag at base -> "Protect the flag"
     * - Flag stolen/dropped -> "Recover the flag"
     *
     * Attack role:
     * - You carry enemy flag -> "Capture the flag"
     * - Teammate carries enemy flag -> "Help the carrier"
     * - Enemy flag at base or dropped -> "Steal the flag"
     */
    public String computeObjectiveText(Player player) {
        Role role = this.getRole(player);
        if (role == null) {
            return null;
        }

        var teamManager = this.battle.getGame().getTeamManager();
        BattleTeam myTeam = teamManager.getTeam(player);
        if (myTeam == null) {
            return null;
        }

        // Current implementation assumes standard 2-team CTF (my team vs one enemy team)
        BattleTeam enemy = teamManager.getTeams().stream()
                .filter(t -> t != myTeam)
                .findFirst()
                .orElse(null);

        if (enemy == null) {
            return null;
        }

        if (role == Role.DEFENSE) {
            var flag = myTeam.getFlag();

            if (flag.isHome()) {
                return "Defender a bandeira!";
            }

            if (flag.getCarrier() != null &&
                    teamManager.getTeam(flag.getCarrier()) == enemy) {
                // Enemy is carrying our flag
                return "Recuperar a bandeira!";
            }

            if (flag.isDropped()) {
                return "Recuperar a bandeira!";
            }

            // Fallback
            return "Defender a bandeira!";
        }

        if (role == Role.ATTACK) {
            var flag = enemy.getFlag();

            // You carry the enemy flag
            if (flag.getCarrier() != null && flag.getCarrier().equals(player)) {
                return "Capture a bandeira!";
            }

            // Teammate carries the enemy flag
            if (flag.getCarrier() != null &&
                    teamManager.getTeam(flag.getCarrier()) == myTeam) {
                return "Ajudar o portador!";
            }

            // Enemy flag at base or dropped
            if (flag.isHome() || flag.isDropped()) {
                return "Roubar a bandeira!";
            }

            // Fallback
            return "Roubar a bandeira!";
        }

        return null;
    }

    /**
     * Called by listeners when something that might change objectives happens.
     * Sends a chat message only if the line actually changed.
     */
    public void checkAndAnnounceObjective(Player player) {
        String text = this.computeObjectiveText(player);
        if (text == null) {
            return;
        }

        String line = C.WHITE + "Objective: " + C.R + text;

        UUID id = player.getUniqueId();
        String old = this.lastObjectiveAnnouncement.get(id);
        if (!line.equals(old)) {
            this.lastObjectiveAnnouncement.put(id, line);
            player.sendMessage(line);
        }
    }

    // ---- Event wiring ----

    @EventHandler
    public void onParticipate(PlayerParticipateEvent event) {
        this.loadRole(event.getPlayer());
        this.updateTabName(event.getPlayer());

        if (!AUTO_OPEN_ROLE_GUI)
            return;

        BattleGameState state = this.battle.getGame().getState();
        if (state == BattleGameState.BEFORE || state == BattleGameState.DURING)
            this.battle.getRolePreferenceGui().open(event.getPlayer());
    }



    @EventHandler
    public void onResign(PlayerResignEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.roles.remove(id);
        this.lastObjectiveAnnouncement.remove(id);
    }

    @EventHandler
    public void onJoinTeam(PlayerJoinTeamEvent event) {
        this.updateTabName(event.getPlayer());
        //this.checkAndAnnounceObjective(event.getPlayer());
    }
}
