package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Log4j2
public class SoldierKit extends BattleKit {

    private static final List<Material> UNCLIMABLE = List.of(
            Material.COAL_ORE, Material.BARRIER, Material.QUARTZ_ORE);
    private static final float PER_CLIMB = 1.0f / 7;
    private static final Duration RESTORE_TIME = Duration.ofSeconds(10);

    public SoldierKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
        player.setExp(1.0f);
    }

    @EventHandler
    public void onTick(TickEvent event) {
        // Restore EXP to the climb bar
        float per = 1f / (RESTORE_TIME.toSeconds() * 20);
        getPlayer().setExp(Math.min(getPlayer().getExp() + per, 1));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() == getPlayer() && event.getCause() == DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    private void onClick(PlayerInteractEvent e) {
        if (e.getPlayer() != getPlayer()) {
            return;
        }

        if (!EventUtil.isRightClick(e) || e.getClickedBlock() == null) {
            return;
        }

        if (UNCLIMABLE.contains(e.getClickedBlock().getType())) {
            return;
        }

        if (getPlayer().getExp() < PER_CLIMB) {
            return;
        }

        boolean delay = true;

        // Push the player away from any wall they are near
        // This prevents them from getting stuck
        Player player = getPlayer();
        if (player.getLocation().add(0, 0, 0.35).getBlock().getType() != Material.AIR) {
            player.setVelocity(new Vector(0, 0, -1));
        } else if (player.getLocation().add(0, 0, -0.35).getBlock().getType() != Material.AIR) {
            player.setVelocity(new Vector(0, 0, 1));
        } else if (player.getLocation().add(0.35, 0, 0).getBlock().getType() != Material.AIR) {
            player.setVelocity(new Vector(-1, 0, 0));
        } else if (player.getLocation().add(-0.35, 0, 0).getBlock().getType() != Material.AIR) {
            player.setVelocity(new Vector(1, 0, 0));
        } else {
            delay = false;
        }

        if (delay) {
            attach(Bukkit.getScheduler().runTask(plugin, () -> {
                player.setVelocity(new Vector(0, 1.0f, 0));
            }));
        } else {
            player.setVelocity(new Vector(0, 1.0f, 0));
        }

        player.setExp(player.getExp() - PER_CLIMB);
    }

    @Override
    public String getName() {
        return "Soldier";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
                new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.IRON_CHESTPLATE),
                new ItemStack(Material.IRON_HELMET),
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        KitItem sword = new KitItem(this, ItemBuilder.of(Material.IRON_SWORD).name("Wall Climbing Sword").build());
        sword.onInteract(this::onClick);

        return new KitInventoryBuilder()
                .add(sword)
                .addFood(4)
                .addCompass(8)
                .build();
    }

}
