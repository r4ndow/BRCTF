package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.chat.C;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Map;

public class ChemistKit extends BattleKit {

    private static final Duration REFILL_TIME = Duration.seconds(7);
    private static final List<PotionEffectType> HARMFUL_POTION_EFFECT_TYPES = List.of(
        PotionEffectType.HARM,
        PotionEffectType.POISON
    );

    private PotionItem healPotion;

    public ChemistKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
        this.getPlayer().setExp(1f);
    }

    @Override
    public String getName() {
        return "Chemist";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            new ItemStack(Material.LEATHER_BOOTS),
            ItemBuilder.of(Material.GOLD_LEGGINGS).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).build(),
            ItemBuilder.of(Material.GOLD_CHESTPLATE).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 2).build(),
            new ItemStack(Material.LEATHER_HELMET)
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        return new KitInventoryBuilder()
            .add(ItemBuilder.of(Material.IRON_SWORD)
                .name("Chemist Sword")
                .enchant(Enchantment.DAMAGE_ALL, 1)
                .unbreakable())
            .add(new PotionItem(
                ItemBuilder.potion()
                    .effect(PotionType.INSTANT_DAMAGE)
                    .splash()
                    .amount(12)
                    .name("Instant Damage"),
                1f / 4 + 0.125
            ))
            .add(new PotionItem(
                ItemBuilder.potion()
                    .effect(PotionType.POISON, Duration.seconds(6), 1)
                    .splash()
                    .amount(8)
                    .name("Poison II"),
                0
            ))
            .add(new PotionItem(
                ItemBuilder.potion()
                    .effect(PotionType.JUMP, Duration.seconds(5))
                    .effect(PotionEffectType.JUMP, Duration.seconds(4), 1)
                    .splash()
                    .amount(3)
                    .name("Jump Boost II"),
                0
            ))
            .add(new PotionItem(
                ItemBuilder.potion()
                    .effect(PotionType.FIRE_RESISTANCE, Duration.mins(3))
                    .effect(PotionType.SPEED, Duration.mins(3))
                    .splash()
                    .amount(5)
                    .name("Buff Pot"),
                0
            ))
            .add(this.healPotion = new PotionItem(
                ItemBuilder.potion()
                    .effect(PotionType.INSTANT_HEAL, 2)
                    .splash()
                    .amount(5)
                    .name("Instant Health III"),
                0
            ))
            .add(new PotionItem(
                ItemBuilder.potion()
                    .effect(PotionType.REGEN, Duration.seconds(16), 2)
                    .splash()
                    .amount(5)
                    .name("Regeneration III"),
                0
            ))
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        float per = 1f / REFILL_TIME.ticks();
        this.getPlayer().setExp(Math.min(this.getPlayer().getExp() + per, 1));
    }

    @EventHandler
    public void onPotionEffect(PotionSplashEvent event) {
        if (event.getPotion().getShooter() != this.getPlayer()) {
            return;
        }

        boolean poison = event.getPotion().getEffects().stream()
            .map(PotionEffect::getType)
            .anyMatch(PotionEffectType.POISON::equals);
        boolean hitEnemy = event.getAffectedEntities().stream()
            .filter(livingEntity -> livingEntity instanceof Player)
            .map(livingEntity -> (Player) livingEntity)
            .anyMatch(this::isEnemy);

        // When a chemist throws a poison potion, and it hits an enemy, it should also impact the thrower and their team
        if (poison && hitEnemy) {
            return;
        }

        boolean harmful = event.getPotion().getEffects().stream()
            .map(PotionEffect::getType)
            .anyMatch(HARMFUL_POTION_EFFECT_TYPES::contains);

        for (LivingEntity affectedEntity : event.getAffectedEntities()) {
            if (!(affectedEntity instanceof Player hit)) {
                return;
            }

            if (this.isTeammate(hit) && harmful) {
                // Harmful effects shouldn't impact teammates
                // Beneficial effects are intentionally allowed to impact enemies
                event.setIntensity(hit, 0);
            }
        }
    }

    @Override
    public void restoreFoodItem() {
        this.healPotion.increment(this.healPotion.getOriginal().getAmount());
    }

    class PotionItem extends KitItem {

        private final float xp;

        public PotionItem(ItemBuilder item, double xp) {
            super(ChemistKit.this, item.build(), true);
            this.xp = (float) xp;
        }

        @EventHandler
        public void onLaunch(ProjectileLaunchEvent event) {
            if (event.getEntity().getShooter() != ChemistKit.this.getPlayer()) {
                return;
            }

            if (!this.isItem(ChemistKit.this.getPlayer().getItemInHand()) || this.isPlaceholder()) {
                return;
            }

            if (ChemistKit.this.getPlayer().getExp() < this.xp) {
                ChemistKit.this.getPlayer().sendMessage(C.warn(C.AQUA) + "Your magic has run dry! You must recharge!");
                event.setCancelled(true);

                // The potion item has already been used even if the event is cancelled
                this.modify(item -> item.amount(this.getItem().getAmount() + 1));
            } else {
                ChemistKit.this.getPlayer().setExp(Math.max(ChemistKit.this.getPlayer().getExp() - this.xp, 0));

                // Because the potion is thrown, we want to increase the number of items first
                this.modify(item -> item.amount(this.getItem().getAmount() + 1));
                // Then we can do a decrement which will handle placeholders
                this.decrement(true);

                // Attach the projectile so it will be removed when this kit is destroyed
                ChemistKit.this.attach(event.getEntity());
            }
        }

    }

}
