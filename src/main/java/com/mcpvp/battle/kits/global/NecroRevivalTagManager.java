package com.mcpvp.battle.kits.global;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.GameDeathEvent;
import com.mcpvp.battle.event.GameRespawnEvent;
import com.mcpvp.battle.kit.item.FoodItem;
import com.mcpvp.battle.kits.MedicKit;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles revival tagging for the Necro kit. Revival tagging is a unique mechanic because it lasts longer than the
 * kit owner. For example, a Necro can tag someone for revival, and it persists even after the Necro dies.
 */
@Getter
@RequiredArgsConstructor
public class NecroRevivalTagManager implements EasyListener {

    private static final Duration TAG_DURATION = Duration.secs(10);

    private final BattlePlugin plugin;
    private final List<Player> tagged = new ArrayList<>();
    private final List<Player> zombies = new ArrayList<>();

    public void setRevivalTagged(Player player) {
        if (this.tagged.contains(player) || this.zombies.contains(player)) {
            return;
        }

        this.tagged.add(player);

        final Expiration expiration = Expiration.after(TAG_DURATION);
        EasyTask.of(holder -> {
            if (!player.isOnline() || !this.isRevivalTagged(player)) {
                holder.cancel();
                return;
            }

            if (expiration.isExpired()) {
                player.sendMessage(C.info(C.AQUA) + "Your revival tag has expired!");
                holder.cancel();
            }
        }).runTaskTimer(this.getPlugin(), 0, 1);
    }

    public void clearRevivalTag(Player player) {
        this.tagged.remove(player);
    }

    public boolean isRevivalTagged(Player player) {
        return this.tagged.contains(player);
    }

    public boolean isZombie(Player player) {
        return this.zombies.contains(player);
    }

    @EventHandler
    public void onDeath(GameDeathEvent event) {
        if (this.tagged.contains(event.getPlayer())) {
            event.setCancelled(true);

            event.getDeathEvent().setKeepInventory(true);
            event.getDeathEvent().setKeepLevel(true);

            this.revive(event);
            this.clearRevivalTag(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(GameRespawnEvent event) {
        this.clearRevivalTag(event.getPlayer());
        this.zombies.remove(event.getPlayer());
    }

    @EventHandler
    public void onHeal(MedicKit.HealEvent event) {
        if (this.zombies.contains(event.getHealed())) {
            this.dezombify(event.getHealed());
        }
    }

    @EventHandler
    public void onWitherDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
            && this.zombies.contains(player)
            && event.getCause() == EntityDamageEvent.DamageCause.WITHER
        ) {
            event.setCancelled(true);
        }
    }

    public void revive(GameDeathEvent death) {
        Player player = death.getPlayer();

        // Return the player to the location they died
        player.setHealth(player.getMaxHealth());
        player.teleport(death.getLocation());

        // "Zombify" the kit
        this.zombify(player);

        // Effects
        ParticlePacket.of(EnumParticle.SMOKE_LARGE).at(player.getLocation()).spread(1.5).count(15).send();
        player.getWorld().playSound(player.getLocation(), Sound.ZOMBIE_REMEDY, 1.0f, 1.0f);
        player.sendMessage(C.info(C.RED) + "You have been saved from death!");

        // Drop the flag if they are holding it
        this.plugin.getBattle().getGame().getTeamManager().getTeams().stream()
            .filter(bt -> bt.getFlag().getCarrier() == player)
            .findFirst()
            .ifPresent(bt -> bt.getFlagManager().drop(player, null));
    }

    private void zombify(Player player) {
        this.plugin.getBattle().getKitManager().find(player).ifPresent(playerKit -> {
            playerKit.getAllItems().forEach(kitItem -> {
                if (kitItem instanceof FoodItem) {
                    kitItem.modify(item -> item.type(Material.ROTTEN_FLESH));
                }
            });
        });

        player.getInventory().setHelmet(ItemBuilder.of(Material.SKULL_ITEM).data(2).build());
        //player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 99999, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 99999, 0));

        this.tagged.remove(player);
        this.zombies.add(player);
    }

    private void dezombify(Player player) {
        this.plugin.getBattle().getKitManager().find(player).ifPresent(playerKit -> {
            playerKit.getAllItems().forEach(kitItem -> {
                if (kitItem instanceof FoodItem) {
                    kitItem.modify(item -> item.type(kitItem.getOriginal().getType()));
                }
            });

            player.getInventory().setHelmet(playerKit.createArmor()[3]);
        });
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.WITHER);

        this.zombies.remove(player);
    }

}
