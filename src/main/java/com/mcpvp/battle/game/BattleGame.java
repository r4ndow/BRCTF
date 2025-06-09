package com.mcpvp.battle.game;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.flag.FlagListener;
import com.mcpvp.battle.flag.FlagMessageBroadcaster;
import com.mcpvp.battle.flag.FlagStatsListener;
import com.mcpvp.battle.game.listener.BattlePermanentGameListener;
import com.mcpvp.battle.game.state.BattleDuringGameStateHandler;
import com.mcpvp.battle.game.state.BattleGameStateHandler;
import com.mcpvp.battle.game.state.BattleOutsideGameStateHandler;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.scoreboard.BattleScoreboardManager;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.team.BattleTeamManager;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.structure.StructureViolation;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

@Log4j2
@Getter
@RequiredArgsConstructor
public class BattleGame extends EasyLifecycle {

    private final BattlePlugin plugin;
    private final Battle battle;
    private final BattleMapData map;
    private final World world;
    private final BattleGameConfig config;
    private final BattleTeamManager teamManager;
    private final BattleScoreboardManager scoreboardManager;
    private final Map<Player, BattleGamePlayerStats> playerStats = new HashMap<>();

    @Nullable
    private BattleGameState state = null;
    private BattleGameStateHandler stateHandler;

    public void setup() {
        log.info("Setup game on map " + map);

        attach(new BattlePermanentGameListener(plugin, this));
        attach(new FlagListener(plugin, this));
        attach(new FlagMessageBroadcaster(plugin));
        attach(new FlagStatsListener(plugin, this));
        attach(scoreboardManager);

        scoreboardManager.init();

        setState(BattleGameState.BEFORE);

        battle.getStructureManager().registerChecker((block) -> {
            if (teamManager.getTeams().stream().anyMatch(bt -> bt.isInSpawn(block.getLocation()))) {
                return Optional.of(new StructureViolation("IN_SPAWN", "You can't place this in spawn"));
            }
            return Optional.empty();
        });
    }

    public void stop() {
        setState(null);
        super.shutdown();
    }

    /**
     * Leaves the current state (if present) and enters the given state (if not
     * null).
     *
     * @param state The state to enter.
     */
    public void setState(BattleGameState state) {
        if (this.state != state) {
            if (this.state != null) {
                leaveState(this.state);
            }

            this.state = state;

            if (state != null) {
                enterState(state);
            }
        }
    }

    private void enterState(BattleGameState state) {
        this.stateHandler = switch (state) {
            case BEFORE, AFTER -> new BattleOutsideGameStateHandler(plugin, this);
            case DURING -> new BattleDuringGameStateHandler(plugin, this);
        };
        this.stateHandler.enterState();

        fireParticipateEvents();
    }

    private void leaveState(BattleGameState state) {
        if (this.stateHandler != null) {
            this.stateHandler.leaveState();
        }
        this.stateHandler = null;
    }

    /**
     * Respawns the player **during the game**. Not before or after.
     *
     * @param player The player to respawn.
     */
    public void respawn(Player player, boolean died) {
        // Drop the flag if they have it
        teamManager.getTeams().forEach(bt -> {
            if (bt.getFlag().getCarrier() == player) {
                bt.getFlagManager().drop(player, null);
            }
        });

        // Death animation
        if (died) {
            doDeathAnimation(player);

            // Adjust stats
            editStats(player, s -> {
                s.setBestStreak(Math.max(s.getBestStreak(), s.getStreak()));
                s.setStreak(0);
                s.setDeaths(s.getDeaths() + 1);
            });
        }

        // Reset negative statues
        player.setHealth(player.getMaxHealth());
        player.setFireTicks(0);
        player.setExp(0);

        // Clear inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // Teleport to spawn
        BattleTeam team = getTeamManager().getTeam(player);
        Location spawn = getConfig().getTeamConfig(team).getSpawn();
        player.teleport(spawn);
        player.setVelocity(new Vector());

        // Players must be teleported immediately on death to avoid the death screen
        // But there needs to be a tick delay before equipping the kit due to inventory resets
        attach(Bukkit.getScheduler().runTask(plugin, () -> {
            battle.getKitManager().createSelected(player);
        }));
    }

    public void remove(Player player) {
        // Remove kit
        Kit kit = battle.getKitManager().get(player);
        if (kit != null) {
            kit.shutdown();
        }

        // Remove player from team
        getTeamManager().setTeam(player, null);
    }

    private void doDeathAnimation(Player player) {
        player.playEffect(EntityEffect.HURT);
        LivingEntity skeleton = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), EntityType.SKELETON);
        skeleton.damage(100);
    }

    private void fireParticipateEvents() {
        for (Player player : getParticipants()) {
            new PlayerParticipateEvent(player, this).call();
        }
    }

    public boolean isParticipant(Player player) {
        return player.getGameMode() == GameMode.SURVIVAL;
    }

    public Collection<? extends Player> getParticipants() {
        return Bukkit.getOnlinePlayers().stream()
                .filter(this::isParticipant)
                .toList();
    }

    public void editStats(Player player, Consumer<BattleGamePlayerStats> operator) {
        operator.accept(getStats(player));
    }

    @NonNull
    public BattleGamePlayerStats getStats(Player player) {
        return playerStats.computeIfAbsent(player, k -> new BattleGamePlayerStats());
    }

    /**
     * @return The team that won this game (e.g. by meeting the required number of caps).
     * Returns null if no team has won.
     */
    @Nullable
    public BattleTeam getWinner() {
        return teamManager.getTeams().stream()
                .filter(t -> t.getCaptures() == config.getCaps())
                .findFirst()
                .orElse(null);
    }

}
