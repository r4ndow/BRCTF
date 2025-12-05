package com.mcpvp.battle.game.state;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.*;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameState;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitSelectedEvent;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.util.PlayerUtil;
import com.mcpvp.common.util.nms.TitleUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public class BattleDuringGameStateHandler extends BattleGameStateHandler {

    private boolean introBroadcasted = false;
    private final Set<UUID> introSeen = new HashSet<>();


    public BattleDuringGameStateHandler(BattlePlugin plugin, BattleGame game) {
        super(plugin, game);
    }

    private String colorizeIntroLine(Player player, String line) {
        BattleTeam team = this.game.getTeamManager().getTeam(player);
        String teamColor;

        if (team != null && team.getColor() != null) {
            // Colors#getChat is already used elsewhere for team-colored names/messages
            teamColor = team.getColor().getChatString();
        } else {
            // Fallback to the old default color
            teamColor = C.AQUA;
        }

        return line.replace(C.AQUA, teamColor);
    }


    private String[] getIntroLines() {
        return new String[]{
                "",
                C.AQUA + C.B + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
                "                                      " + C.AQUA + C.B + "CTF",
                "",
                "       " + C.YELLOW + "Roube a bandeira inimiga e traga-a até sua própria",
                "        " + C.YELLOW + "bandeira para capturá-la. Defenda sua " + C.YELLOW + "bandeira",
                "            " + C.YELLOW + "   e recupere-a caso seja roubada. ",
                "",
                C.AQUA + C.B + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
        };
    }


    private void broadcastIntro() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.sendIntro(player);
        }
    }

    private void sendIntro(Player player) {
        for (String line : this.getIntroLines()) {
            player.sendMessage(this.colorizeIntroLine(player, line));
        }
    }


    @Override
    public void enterState() {
        super.enterState();
        this.game.getTeamManager().getTeams().forEach(bt -> bt.getFlag().setLocked(false));
        this.game.getBattle().getMatch().getTimer().setSeconds(this.game.getConfig().getTime() * 60);
        this.game.getBattle().getMatch().getTimer().setPaused(false);

        this.broadcastIntro();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 0.7f, 1.0f);
            }, 2L);
        }
        this.game.getParticipants().forEach(player -> this.introSeen.add(player.getUniqueId()));
    }


    @Override
    public void leaveState() {
        // Shut down all kits
        this.game.getParticipants().forEach(player -> {
            Optional.ofNullable(this.game.getBattle().getKitManager().get(player)).ifPresent(Kit::shutdown);
            PlayerUtil.reset(player);
        });

        super.leaveState();

        // Send a summary message
        List<String> summary = new ArrayList<>();
        summary.add(" ");
        summary.add(C.YELLOW + "✦ " + C.GOLD + "✦ " + C.b(C.R) + "GAME SUMMARY" + C.YELLOW + " ✦" + C.GOLD + " ✦");

        BattleTeam leader = this.game.getLeader().orElse(null);
        if (leader == null) {
            summary.add(C.info(C.GOLD) + "Nobody won!");
        } else {
            BattleTeam loser = this.game.getTeamManager().getNext(leader);
            summary.add("%sWinner: %s (%s - %s)".formatted(
                C.info(C.GOLD),
                leader.getColoredName() + C.GRAY,
                leader.getColor().toString() + leader.getCaptures() + C.GRAY,
                loser.getColor().toString() + loser.getCaptures() + C.GRAY
            ));
        }
        summary.add(C.info(C.GOLD) + C.GRAY + "Map: " + C.WHITE + this.game.getMap().getName());

        summary.forEach(Bukkit::broadcastMessage);

        BattleTeam winner = this.game.getLeader().orElse(null);
        if (winner != null) {
            BattleTeam loser = this.game.getTeamManager().getNext(winner);

            for (Player player : this.game.getParticipants()) {
                BattleTeam team = this.game.getTeamManager().getTeam(player);
                if (team == null) {
                    continue;
                }

                String title;
                if (team.equals(winner)) {
                    title = C.GOLD + C.B + "Victory";
                } else if (team.equals(loser)) {
                    title = C.RED + C.B + "Defeat";
                } else {
                    continue;
                }

                TitleUtil.sendTitle(
                        player,
                        title,
                        "",
                        Duration.seconds(1),
                        Duration.seconds(3),
                        Duration.seconds(1)
                );
            }
        }

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getEntity().setHealth(event.getEntity().getMaxHealth());
        event.setKeepInventory(false);
        event.setKeepLevel(false);
        event.setNewExp(0);
        event.setNewLevel(0);
        event.setDroppedExp(0);
        event.getDrops().clear();

        if (new GameDeathEvent(event.getEntity(), event.getEntity().getLocation().clone(), event).callIsCancelled()) {
            event.setDeathMessage(null);
            return;
        }

        this.game.respawn(event.getEntity(), true);
    }

    @EventHandler
    public void onJoinTeam(PlayerJoinTeamEvent event) {
        this.game.respawn(event.getPlayer(), false);
    }

    @EventHandler
    public void onParticipate(PlayerParticipateEvent event) {
        Player player = event.getPlayer();
        if (this.game.getState() == BattleGameState.DURING && !this.introSeen.contains(player.getUniqueId())) {
            this.sendIntro(player);

            this.introSeen.add(player.getUniqueId());
        }
        this.game.respawn(player, false);
    }



    @EventHandler(priority = EventPriority.MONITOR)
    public void onKitSelected(KitSelectedEvent event) {
        if (event.isRespawn()) {
            this.game.respawn(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // This should not happen, but just to be safe...
        if (this.game.isParticipant(event.getPlayer())) {
            this.game.respawn(event.getPlayer(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageWhileInSpawn(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        BattleTeam team = this.game.getTeamManager().getTeam(player);

        if (team.isInSpawn(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageSameTeam(EntityDamageByEntityEvent event) {
        BattleTeam damagerTeam = null;
        BattleTeam damagedTeam = null;

        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                damagerTeam = this.game.getTeamManager().getTeam(shooter);
            }
        }

        if (event.getDamager() instanceof Player damager) {
            damagerTeam = this.game.getTeamManager().getTeam(damager);
        }

        if (event.getEntity() instanceof Player damaged) {
            damagedTeam = this.game.getTeamManager().getTeam(damaged);
        }

        if (damagedTeam == damagerTeam) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void passiveHealInSpawn(TickEvent event) {
        this.game.getParticipants().forEach(p -> {
            BattleTeam team = this.game.getTeamManager().getTeam(p);
            if (team != null && team.isInSpawn(p) && event.getTick() % 20 == 0) {
                p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 1));
            }
        });
    }

    @EventHandler
    public void killInEnemySpawn(PlayerEnterSpawnEvent event) {
        if (this.game.getTeamManager().getTeam(event.getPlayer()) != event.getTeam()) {
            // If this is caused by teleporting, we need to cancel the teleport
            // Otherwise the player will be respawned, then the teleport will go through
            if (event.getCause() instanceof PlayerTeleportEvent) {
                event.getCause().setCancelled(true);
            }

            this.game.respawn(event.getPlayer(), true);
        }
    }

    @EventHandler
    public void loseFlagInSpawn(PlayerEnterSpawnEvent event) {
        Optional<BattleTeam> carryingFlag = this.game.getTeamManager().getTeams().stream()
            .filter(bt -> bt.getFlag().getCarrier() == event.getPlayer())
            .findAny();
        carryingFlag.ifPresent(battleTeam -> battleTeam.getFlagManager().restore());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCapture(FlagCaptureEvent event) {
        if (this.game.getWinner().isPresent()) {
            this.game.setState(BattleGameState.AFTER);
        }
    }

    @EventHandler
    public void onPlayerKillPlayer(PlayerKilledByPlayerEvent event) {
        this.game.editStats(event.getKiller(), s -> {
            s.setKills(s.getKills() + 1);
            s.setStreak(s.getStreak() + 1);
        });
    }

    @EventHandler
    public void onLandOnSoulSandOrHay(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        Block blockBelow = event.getEntity()
                .getLocation()
                .getBlock()
                .getRelative(BlockFace.DOWN);

        Material type = blockBelow.getType();

        // If it's neither soul sand nor hay bale, do nothing
        if (type != Material.SOUL_SAND && type != Material.HAY_BLOCK) {
            return;
        }

        event.setCancelled(true);
    }


}
