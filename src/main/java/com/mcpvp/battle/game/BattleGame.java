package com.mcpvp.battle.game;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.config.BattleCallout;
import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.chat.BattleDeathMessageHandler;
import com.mcpvp.battle.event.GameRespawnEvent;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.flag.*;
import com.mcpvp.battle.game.state.BattleDuringGameStateHandler;
import com.mcpvp.battle.game.state.BattleGameStateHandler;
import com.mcpvp.battle.game.state.BattleOutsideGameStateHandler;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.scoreboard.BattleScoreboardManager;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.team.BattleTeamManager;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.util.PlayerUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Log4j2
@Getter
@RequiredArgsConstructor
public class BattleGame extends EasyLifecycle {

    private static final double CALLOUT_RADIUS = 15;

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
        log.info("Setup game on map {}", this.map);

        this.attach(new BattleGameListener(this.plugin, this));
        this.attach(new BattleDeathMessageHandler(this.plugin));
        this.attach(new FlagListener(this.plugin, this));
        this.attach(new FlagMessageBroadcaster(this.plugin));
        this.attach(new FlagStatsListener(this.plugin, this));
        this.attach(this.scoreboardManager);

        switch (this.getBattle().getOptions().getGame().getFlagType()) {
            case WOOL: {
                this.teamManager.getTeams().forEach(bt -> {
                    WoolFlag flag = new WoolFlag(this.plugin, bt);
                    this.attach(flag);
                    bt.setFlag(flag);
                    bt.setFlagManager(new FlagManager(flag));
                });
                break;
            }
            case BANNER: {
                this.teamManager.getTeams().forEach(bt -> {
                    BannerFlag flag = new BannerFlag(this.plugin, bt);
                    this.attach(flag);
                    bt.setFlag(flag);
                    bt.setFlagManager(new FlagManager(flag));
                });
            }
        }

        this.scoreboardManager.init();

        this.setState(BattleGameState.BEFORE);
    }

    public void stop() {
        this.setState(null);
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
                this.leaveState();
            }

            this.state = state;

            if (state != null) {
                this.enterState(state);
            }
        }
    }

    private void enterState(BattleGameState state) {
        this.stateHandler = switch (state) {
            case BEFORE, AFTER -> new BattleOutsideGameStateHandler(this.plugin, this, state);
            case DURING -> new BattleDuringGameStateHandler(this.plugin, this);
        };
        this.stateHandler.enterState();

        this.fireParticipateEvents();
    }

    private void leaveState() {
        if (this.stateHandler != null) {
            this.stateHandler.leaveState();
        }
        this.stateHandler = null;
    }

    /**
     * Respawns the player **during the game**. Not before or after.
     *
     * @param player The player to respawn.
     * @param respawn Whether to respawn them.
     */
    public void respawn(Player player, boolean died, boolean respawn) {
        // Drop the flag if they have it
        this.teamManager.getTeams().forEach(bt -> {
            if (bt.getFlag().getCarrier() == player) {
                bt.getFlagManager().drop(player, null);
            }
        });

        // Death animation
        if (died) {
            this.doDeathAnimation(player);

            // Adjust stats
            this.editStats(player, s -> {
                s.setBestStreak(Math.max(s.getBestStreak(), s.getStreak()));
                s.setStreak(0);
                s.setDeaths(s.getDeaths() + 1);
            });
        }

        // Teleport to spawn
        if (respawn) {
            BattleTeam team = this.getTeamManager().getTeam(player);
            Location spawn = this.getConfig().getTeamConfig(team).getSpawn();

            player.setHealth(player.getMaxHealth());
            player.setFireTicks(0);
            player.setExp(0);
            PlayerUtil.setAbsorptionHearts(player, 0);

            // Clear inventory
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);

            player.setHealth(player.getMaxHealth());
            player.teleport(spawn);
            player.setVelocity(new Vector());

            EasyTask.of(() -> {
                if (!player.isOnline()) {
                    return;
                }

                // A small amount of velocity carries over for some reason
                player.setVelocity(new Vector());

                // Kit application needs to be done later due to inventory clearing
                this.battle.getKitManager().createSelected(player);

                // Respawn is finished
                new GameRespawnEvent(player).call();
            }).runTaskLater(this.getPlugin(), 0);
        }
    }

    public void remove(Player player) {
        // Remove kit
        Kit kit = this.battle.getKitManager().get(player);
        if (kit != null) {
            kit.shutdown();
        }

        // Remove player from team
        this.getTeamManager().setTeam(player, null);
    }

    private void doDeathAnimation(Player player) {
        player.playEffect(EntityEffect.HURT);
        LivingEntity skeleton = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), EntityType.SKELETON);
        skeleton.damage(100);
    }

    private void fireParticipateEvents() {
        for (Player player : this.getParticipants()) {
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

    public List<? extends Player> getSpectators() {
        return Bukkit.getOnlinePlayers().stream().filter(Predicate.not(this::isParticipant)).toList();
    }

    public void editStats(Player player, Consumer<BattleGamePlayerStats> operator) {
        operator.accept(this.getStats(player));
    }

    public Optional<BattleCallout> findClosestCallout(Location location) {
        return this.getConfig().getCallouts().stream()
            .filter(callout -> callout.getLocation().distance(location) <= CALLOUT_RADIUS)
            .min(Comparator.comparingDouble(c -> c.getLocation().distanceSquared(location)));
    }

    @NonNull
    public BattleGamePlayerStats getStats(Player player) {
        return this.playerStats.computeIfAbsent(player, k -> new BattleGamePlayerStats());
    }

    /**
     * @return The team that won this game (e.g. by meeting the required number of caps).
     * Returns null if no team has won.
     */
    @Nullable
    public BattleTeam getWinner() {
        return this.teamManager.getTeams().stream()
            .filter(t -> t.getCaptures() == this.config.getCaps())
            .findFirst()
            .orElse(null);
    }

}
