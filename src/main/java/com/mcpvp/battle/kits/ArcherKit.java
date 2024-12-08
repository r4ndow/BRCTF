package com.mcpvp.battle.kits;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.event.EasyCancellableEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class ArcherKit extends BattleKit {

	private static final int HEADSHOT_DIST = 30;

    public ArcherKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Archer";
    }

    @Override
    public ItemStack[] getArmor() {
        return new ItemStack[] {
                new ItemStack(Material.CHAINMAIL_BOOTS),
                new ItemStack(Material.CHAINMAIL_LEGGINGS),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE),
                new ItemStack(Material.CHAINMAIL_HELMET)
        };
    }

    @Override
    public Map<Integer, ItemStack> getItems() {
        return new KitInventoryBuilder()
                .add(Material.STONE_SWORD)
                .add(Material.BOW)
                .add(Material.ARROW)
                .addFood(3)
                .build();
    }

    @EventHandler
    public void onFireArrow(ProjectileLaunchEvent event) {
        if (isPlayer(event.getEntity().getShooter())) {
            getBattle().getProjectileManager().register(event.getEntity()).onHitEvent(this::onHit);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = false)
    @AllArgsConstructor
    public static class ArcherSnipeEvent extends EasyCancellableEvent {

        private final Player shooter;
        private final Player hit;

    }

    private boolean attemptSnipe(Player hit, int distance) {
        return !new ArcherSnipeEvent(this.getPlayer(), hit).call();
    }

	private void onHit(EntityDamageByEntityEvent event) {
		if (event.isCancelled())
			return;
		
		if (!(event.getDamager() instanceof Arrow arrow))
			return;
		
		if (!(arrow.getShooter() instanceof Player shooter))
			return;
		
		if (!(event.getEntity() instanceof LivingEntity hit))
			return;
		
		int distance = (int) hit.getLocation().distance(shooter.getLocation());
		
		if (hit instanceof ArmorStand)
			return;
		
		if (distance < HEADSHOT_DIST)
			return;
		
		if (!attemptSnipe(shooter, distance))
			return;
		
		event.setDamage(1000);
	}

}
