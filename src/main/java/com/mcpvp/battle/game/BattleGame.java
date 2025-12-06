package com.mcpvp.battle.game;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.chat.BattleDeathMessageHandler;
import com.mcpvp.battle.config.BattleCallout;
import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.event.GameRespawnEvent;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.flag.*;
import com.mcpvp.battle.game.state.BattleDuringGameStateHandler;
import com.mcpvp.battle.game.state.BattleGameStateHandler;
import com.mcpvp.battle.game.state.BattleOutsideGameStateHandler;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.options.BattleOptionsInput;
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

    public void setup(Map<BattleTeam, Set<Player>> playerMap) {
        log.info("Setup game on map {}", this.map);

        this.attach(new BattleGameListener(this.plugin, this));
        this.attach(new BattleDeathMessageHandler(this.plugin));
        this.attach(new FlagListener(this.plugin, this));
        this.attach(new FlagMessageBroadcaster(this.plugin));
        this.attach(new FlagStatsListener(this.plugin, this));
        this.attach(new FlagMatchPointNotifier(this.plugin, this));

        this.attach(this.scoreboardManager);

        this.setupFlags(this.battle.getOptions().getGame().getFlagType());

        this.scoreboardManager.init();

        // Transfer players from the old game
        playerMap.forEach((oldTeam, players) -> {
            BattleTeam nextTeam = this.getTeamManager().getTeam(oldTeam.getId());
            players.forEach(player -> this.getTeamManager().setTeam(player, nextTeam));
        });

        this.setState(BattleGameState.BEFORE);
    }

    public void stop() {
        this.setState(null);
        super.shutdown();
    }

    public void setupFlags(BattleOptionsInput.FlagType flagType) {
        this.teamManager.getTeams().forEach(bt -> {
            if (bt.getFlag() != null) {
                bt.getFlag().remove();
                bt.getFlag().unregister();
            }

            switch (flagType) {
                case WOOL -> {
                    WoolFlag flag = new WoolFlag(this.plugin, bt);
                    this.attach(flag);
                    bt.setFlag(flag);
                    bt.setFlagManager(new FlagManager(flag));
                }
                case BANNER -> {
                    BannerFlag flag = new BannerFlag(this.plugin, bt);
                    this.attach(flag);
                    bt.setFlag(flag);
                    bt.setFlagManager(new FlagManager(flag));
                }
            }
        });
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
     */
    public void respawn(Player player, boolean died) {
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

        // Most player related activities should happen in the next game tick
        // Doing stuff in the same tick as the player dying causes lots of weird issues
        EasyTask.of(() -> {
            if (!player.isOnline()) {
                return;
            }

            // Teleport to spawn
            BattleTeam team = this.getTeamManager().getTeam(player);
            Location spawn = this.getConfig().getTeamConfig(team).getSpawn();
            player.teleport(spawn);

            // Reset health, potion effects, etc
            PlayerUtil.reset(player);

            // Sound effect for death needs to be done after teleporting
            if (died) {
                player.playSound(player.getEyeLocation(), Sound.HURT_FLESH, 1.0f, 1.0f);

                Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                    player.playSound(player.getLocation(), Sound.VILLAGER_DEATH, 1.0f, 1.0f);
                }, 1L);
            }

            // Kit application needs to be done later due to inventory clearing
            this.battle.getKitManager().createSelected(player);

            // Respawn is finished
            new GameRespawnEvent(player).call();
        }).runTaskLater(this.getPlugin(), 0);
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
     * Returns an empty Optional if no team has won.
     * @see #getLeader()
     */
    public Optional<BattleTeam> getWinner() {
        return this.teamManager.getTeams().stream()
            .filter(t -> t.getCaptures() == this.config.getCaps())
            .findFirst();
    }

    /**
     * @return The team that is currently in the lead, e.g. they have the most caps. They might not have met the
     * condition to actually end the game yet.
     * @see #getWinner()
     */
    public Optional<BattleTeam> getLeader() {
        // This is the ideal place for tie breaking logic, but just use captures for now
        return this.teamManager.getTeams().stream().max(Comparator.comparingInt(BattleTeam::getCaptures));
    }

}
