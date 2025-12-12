package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.util.nms.ActionbarUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class ArcherKit extends BattleKit {

    private static final int HEADSHOT_DIST = 30;

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
                .add(ItemBuilder.of(Material.STONE_SWORD)
                        .name("Archer Sword")
                        .unbreakable())
                .addFood(4)
                .add(ItemBuilder.of(Material.BOW)
                        .name("Archer Bow")
                        .enchant(Enchantment.ARROW_KNOCKBACK, 1)
                        .unbreakable())
                .add(this.arrows1 = new KitItem(
                        this,
                        ItemBuilder.of(Material.ARROW)
                                .name("Archer Arrows #1")
                                .amount(64)
                                .build()))
                .add(this.arrows2 = new KitItem(
                        this,
                        ItemBuilder.of(Material.ARROW)
                                .name("Archer Arrows #2")
                                .amount(64)
                                .build()))
                .addCompass(8)
                .build();
    }

    @EventHandler
    public void onFireArrow(EntityShootBowEvent event) {
        if (!this.isPlayer(event.getEntity())) {
            return;
        }

        this.attach(new InteractiveProjectile(this.getPlugin(), (Projectile) event.getProjectile())
                .singleEventOnly()
                .onDamageEvent(this::onHit)
        );
        this.attach(event.getProjectile());

        List.of(this.arrows1, this.arrows2).forEach(kitItem -> {
            kitItem.refresh(this.getPlayer().getInventory());
        });

        if (this.arrows1.getItem().getAmount() == 1) {
            this.arrows1.setPlaceholder();
        }
        if (this.arrows2.getItem().getAmount() == 1) {
            this.arrows2.setPlaceholder();
        }
    }

    @EventHandler
    public void onKillFlagCarrier(PlayerKilledByPlayerEvent event) {
        if (event.getKiller() != this.getPlayer()) {
            return;
        }

        BattleTeam team = this.getBattle().getGame().getTeamManager().getTeam(this.getPlayer());
        if (team == null || team.getFlag() == null) {
            return;
        }

        Player carrier = team.getFlag().getCarrier();
        if (carrier != event.getKilled()) {
            return;
        }

        ActionbarUtil.send(this.getPlayer(), C.DARK_PURPLE + "FLAG CARRIER KILL!" + C.R);
        Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> ActionbarUtil.send(this.getPlayer(), ""), 40L);
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
