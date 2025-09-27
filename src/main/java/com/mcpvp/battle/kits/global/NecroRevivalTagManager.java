package com.mcpvp.battle.kits.global;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.GameDeathEvent;
import com.mcpvp.battle.event.GameRespawnEvent;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.item.FoodItem;
import com.mcpvp.battle.kits.MedicKit;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.chat.C;
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
        if (tagged.contains(player) || zombies.contains(player)) {
            return;
        }

        tagged.add(player);

        final Expiration expiration = Expiration.after(TAG_DURATION);
        EasyTask.of(holder -> {
            if (!player.isOnline() || !isRevivalTagged(player)) {
                holder.cancel();
                return;
            }

            if (expiration.isExpired()) {
                player.sendMessage(C.info(C.AQUA) + "Your revival tag has expired!");
                holder.cancel();
            }
        }).runTaskTimer(getPlugin(), 0, 1);
    }

    public void clearRevivalTag(Player player) {
        tagged.remove(player);
    }

    public boolean isRevivalTagged(Player player) {
        return tagged.contains(player);
    }

    public boolean isZombie(Player player) {
        return zombies.contains(player);
    }

    @EventHandler
    public void onDeath(GameDeathEvent event) {
        if (tagged.contains(event.getPlayer())) {
            event.setCancelled(true);

            event.getDeathEvent().setKeepInventory(true);
            event.getDeathEvent().setKeepLevel(true);

            revive(event);
            clearRevivalTag(event.getPlayer());
        }
    }

    @EventHandler
    public void onRespawn(GameRespawnEvent event) {
        clearRevivalTag(event.getPlayer());
    }

    @EventHandler
    public void onHeal(MedicKit.HealEvent event) {
        if (zombies.contains(event.getHealed())) {
            dezombify(event.getHealed());
        }
    }

    @EventHandler
    public void onWitherDamage(EntityDamageEvent event) {
        if (zombies.contains(event.getEntity()) && event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setCancelled(true);
        }
    }

    public void revive(GameDeathEvent death) {
        Player player = death.getPlayer();

        // Return the player to the location they died
        player.setHealth(player.getMaxHealth());
        player.teleport(death.getLocation());

        // "Zombify" the kit
        zombify(player);

        // Effects
        ParticlePacket.of(EnumParticle.SMOKE_LARGE).at(player.getLocation()).spread(1.5).count(15).send();
        player.getWorld().playSound(player.getLocation(), Sound.ZOMBIE_REMEDY, 1.0f, 1.0f);
        player.sendMessage(C.info(C.RED) + "You have been saved from death!");
    }

    private void zombify(Player player) {
        BattleKit kit = plugin.getBattle().getKitManager().get(player);
        kit.getAllItems().forEach(kitItem -> {
            if (kitItem instanceof FoodItem) {
                kitItem.modify(item -> item.type(Material.ROTTEN_FLESH));
            }
        });

        player.getInventory().setHelmet(ItemBuilder.of(Material.SKULL_ITEM).data(2).build());
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 99999, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 99999, 0));

        tagged.remove(player);
        zombies.add(player);
    }

    private void dezombify(Player player) {
        BattleKit kit = plugin.getBattle().getKitManager().get(player);
        kit.getAllItems().forEach(kitItem -> {
            if (kitItem instanceof FoodItem) {
                kitItem.modify(item -> item.type(kitItem.getOriginal().getType()));
            }
        });

        player.getInventory().setHelmet(kit.createArmor()[3]);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.WITHER);

        zombies.remove(player);
    }

}
