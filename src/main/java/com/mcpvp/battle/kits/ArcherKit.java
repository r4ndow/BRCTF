package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.util.chat.C;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ArcherKit extends BattleKit {

    private static final int HEADSHOT_DIST = 35;

    private KitItem arrows1;
    private KitItem arrows2;

    public ArcherKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    public String getName() {
        return "Archer";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            new ItemStack(Material.CHAINMAIL_BOOTS),
            new ItemStack(Material.CHAINMAIL_LEGGINGS),
            new ItemStack(Material.CHAINMAIL_CHESTPLATE),
            new ItemStack(Material.CHAINMAIL_HELMET)
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        return new KitInventoryBuilder()
            .add(ItemBuilder.of(Material.STONE_SWORD).name("Archer Sword").unbreakable())
            .addFood(2)
            .add(ItemBuilder.of(Material.BOW).enchant(Enchantment.ARROW_KNOCKBACK, 1))
            .add(arrows1 = new KitItem(this,
                ItemBuilder.of(Material.ARROW).amount(64).name("Archer Arrows #1").build()))
            .add(arrows2 = new KitItem(this,
                ItemBuilder.of(Material.ARROW).amount(64).name("Archer Arrows #2").build()))
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onFireArrow(EntityShootBowEvent event) {
        if (isPlayer(event.getEntity())) {
            attach(new InteractiveProjectile(getPlugin(), (Projectile) event.getProjectile())
                .singleEventOnly()
                .onDamageEvent(this::onHit)
            );
            attach(event.getEntity());

            if (arrows1.getItem().getAmount() == 1) {
                arrows1.setPlaceholder();
            }
            if (arrows2.getItem().getAmount() == 1) {
                arrows2.setPlaceholder();
            }
        }
    }

    private void onHit(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamager() instanceof Arrow arrow)) {
            return;
        }

        if (!(arrow.getShooter() instanceof Player shooter)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity hit)) {
            return;
        }

        int distance = (int) hit.getLocation().distance(shooter.getLocation());

        if (hit instanceof ArmorStand) {
            return;
        }

        if (distance < HEADSHOT_DIST) {
            return;
        }

        event.setDamage(1000);

        hit.sendMessage(C.info(C.GOLD) + "You were sniped by " + C.highlight(shooter.getName()) + " from " + distance + " blocks!");
        shooter.sendMessage(C.info(C.GOLD) + "You sniped " + C.highlight(hit.getName()) + " from " + distance + " blocks!");
    }

}
