package com.mcpvp.battle.kits.global;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.GameRespawnEvent;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EffectUtil;
import com.mcpvp.common.chat.C;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles death tagging for the Scout kit. Death tagging is a unique mechanic because it lasts longer than the
 * kit owner. For example, a Scout can tag someone for death, and it persists even after the Scout dies.
 */
@Getter
@RequiredArgsConstructor
public class ScoutDeathTagManager implements EasyListener {

    private static final Duration TAG_COOLDOWN = Duration.seconds(15);
    private static final Duration TAG_DURATION = Duration.secs(8);
    private static final Duration TAG_TIME = Duration.mins(5);
    private static final double MIN_DAMAGE_MULT = 1.2;
    private static final double MAX_DAMAGE_MULT = 1.75;

    private final BattlePlugin plugin;
    private final Map<Player, Expiration> deathTagged = new HashMap<>();

    public boolean setDeathTagged(Player player) {
        if (this.isDeathTagged(player)) {
            return false;
        }

        this.deathTagged.computeIfAbsent(player, p -> new Expiration()).expireIn(TAG_COOLDOWN);

        EffectUtil.sendBorderEffect(player);
        player.sendMessage(C.warn(C.GOLD) + "You are now death tagged for " + C.hl("" + TAG_DURATION.seconds()) + " seconds!");

        EasyTask.of(() -> this.removeDeathTag(player)).runTaskLater(this.getPlugin(), TAG_DURATION.ticks());
        return true;
    }

    private void removeDeathTag(Player player) {
        this.deathTagged.remove(player);

        EffectUtil.resetBorderEffect(player);

        player.sendMessage(C.warn(C.GOLD) + "You are no longer death tagged");
    }

    public boolean isDeathTagged(Player player) {
        return this.deathTagged.containsKey(player) && !this.deathTagged.get(player).isExpired();
    }

    @EventHandler
    public void onDamageToDeathTaggedPlayer(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player hit)) {
            return;
        }

        if (!this.isDeathTagged(hit)) {
            return;
        }

        event.setDamage(event.getDamage() * this.getDamageAmplifier(hit));
    }

    @EventHandler
    public void onTick(TickEvent event) {
        this.deathTagged.keySet().forEach(player -> {
            ParticlePacket.blockDust(Material.REDSTONE_BLOCK)
                .at(player.getLocation())
                .count(getParticleCount(this.getDamageAmplifier(player)))
                .setOffY(1)
                .send();
        });
    }

    @EventHandler
    public void onGameRespawn(GameRespawnEvent event) {
        if (this.isDeathTagged(event.getPlayer())) {
            this.removeDeathTag(event.getPlayer());
        }
    }

    private double getDamageAmplifier(Player player) {
        for (BattleTeam team : this.plugin.getBattle().getGame().getTeamManager().getTeams()) {
            if (team.getFlag().getCarrier() == player) {
                return getTemporalDamageAmplifier(System.currentTimeMillis() - team.getFlag().getStolenAt());
            }
        }

        return 1.5;
    }

    /**
     * Creates a sigmoid damage curve that increases over time
     *
     * @return Damage multiplier
     */
    public static double getTemporalDamageAmplifier(long t) {
        double T = TAG_TIME.toMilliseconds(); // time to rise
        double a = MAX_DAMAGE_MULT - MIN_DAMAGE_MULT;

        return MIN_DAMAGE_MULT + a / (1 + Math.exp(5 * (1 - 2 * t / T)));
    }

    public static int getParticleCount(double dmg) {
        int min = 2;
        int max = 8;

        double xmin = MIN_DAMAGE_MULT;

        return (int) Math.floor((max - min) / (MAX_DAMAGE_MULT - xmin) * (dmg - xmin) + min);
    }

}
