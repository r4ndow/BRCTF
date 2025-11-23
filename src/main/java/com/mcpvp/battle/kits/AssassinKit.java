package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kits.global.AssassinCooldownManager;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.task.DrainExpBarTask;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.task.FillExpBarTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;

public class AssassinKit extends BattleKit {

    private static final Duration STRONG_TIME = Duration.seconds(2);
    private static final Duration VULNERABLE_TIME = Duration.seconds(2);
    private static final Duration SPEEDY_TIME = Duration.seconds(6);
    private static final Duration STRONG_RESTORE = Duration.seconds(15);
    private static final Duration STRENGTH_II_TIME = Duration.seconds(9);
    private static final Duration SPEEDY_RESTORE = Duration.seconds(16);
    private static final int SUGAR_AMOUNT = 2;
    private static final List<EntityDamageEvent.DamageCause> IGNORE_WHILE_VULNERABLE = List.of(
        EntityDamageEvent.DamageCause.FIRE,
        EntityDamageEvent.DamageCause.FIRE_TICK,
        EntityDamageEvent.DamageCause.FALL
    );

    private final AssassinCooldownManager cooldownManager;
    private boolean strong;
    private boolean vulnerable;
    private KitItem redstone;
    private KitItem sugar;
    private BukkitTask endStrengthTask;
    private BukkitTask adrenalineFadeMessageTask;

    public AssassinKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
        this.cooldownManager = this.getBattle().getKitManager().getAssassinCooldownManager();
        this.restorePreviousCooldown();
    }

    @Override
    public String getName() {
        return "Assassin";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            new ItemStack(Material.GOLD_BOOTS),
            null,
            null,
            null
        };
    }


    @Override
    public Map<Integer, KitItem> createItems() {
        KitItem sword = new KitItem(
            this,
            ItemBuilder.of(Material.GOLD_SWORD)
                .name("Assassin Sword")
                .enchant(Enchantment.DAMAGE_ALL, 2)
                .unbreakable()
                .build()
        );
        this.redstone = new KitItem(
            this,
            ItemBuilder.of(Material.REDSTONE)
                .name("Assassinate")
                .build()
        );
        this.sugar = new KitItem(
            this,
            ItemBuilder.of(Material.SUGAR)
                .name("Speed Boost")
                .amount(SUGAR_AMOUNT)
                .build()
        );

        this.redstone.onInteract(event -> {
            if (EventUtil.isRightClick(event) && !this.redstone.isPlaceholder()) {
                this.activateStrength(sword);
            }
        });

        this.sugar.onInteract(event -> {
            if (EventUtil.isRightClick(event) && !this.sugar.isPlaceholder()) {
                this.activateSpeed();
            }
        });

        return new KitInventoryBuilder()
            .add(sword)
            .add(this.redstone)
            .add(this.sugar)
            .addCompass(8)
            .build();
    }

    private void restorePreviousCooldown() {
        this.cooldownManager.getCooldownRemaining(this.getPlayer()).ifPresentOrElse(remaining -> {
            this.redstone.setPlaceholder();
            this.getPlayer().setExp(Expiration.after(remaining).getCompletionPercent(STRONG_RESTORE));
            this.animateExp(new FillExpBarTask(this.getPlayer(), remaining));
            this.attach(EasyTask.of(this.redstone::restore).runTaskLater(this.getPlugin(), remaining.ticks()));
        }, () -> {
            this.getPlayer().setExp(1f);
        });
    }

    private void activateStrength(KitItem sword) {
        // Swap to the sword slot
        int slot = this.getPlayer().getInventory().first(sword.getItem());
        if (slot < 9 && slot > -1) {
            this.getPlayer().getInventory().setHeldItemSlot(slot);
        }

        // Activate
        this.getPlayer().sendMessage(C.info(C.AQUA) + "You are now in " + C.hl("Assassin mode") + "!");
        this.strong = true;
        this.vulnerable = true;
        this.redstone.decrement(true);

        this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, VULNERABLE_TIME.ticks(), 0));
        this.getPlayer().setExp(1);

        // Empty XP bar
        this.animateExp(new DrainExpBarTask(this.getPlayer(), STRONG_TIME));

        // Task to restore the redstone
        this.attach(Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
            this.redstone.increment(1);
        }, STRONG_TIME.add(STRONG_RESTORE).ticks()));

        // Task to end the strength
        this.attach(this.endStrengthTask = Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
            this.getPlayer().sendMessage(C.info(C.AQUA) + "Your strength fades...");
            this.strong = false;
            this.animateExp(new FillExpBarTask(this.getPlayer(), STRONG_RESTORE));
            // Override the existing cooldown in case strength ends early
            this.cooldownManager.storeCooldown(this.getPlayer(), STRONG_RESTORE);
        }, STRONG_TIME.ticks()));

        // Task to end vulnerability
        this.attach(Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
            this.getPlayer().sendMessage(C.info(C.AQUA) + "You are no longer vulnerable");
            this.vulnerable = false;
        }, VULNERABLE_TIME.ticks()));

        // Persist the time until redstone is available
        this.cooldownManager.storeCooldown(this.getPlayer(), STRONG_RESTORE.add(STRONG_TIME));
    }

    private void activateSpeed() {
        this.sugar.decrement(true);

        // Add speed effect
        PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, SPEEDY_TIME.ticks(), 2);
        this.getPlayer().addPotionEffect(effect, true);

        // Restore the item
        this.attach(Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
            this.sugar.increment(SUGAR_AMOUNT);
        }, SPEEDY_RESTORE.ticks()));
    }

    private void giveAbsorption() {
        int tier = this.getPlayer().getActivePotionEffects().stream()
            .filter(potionEffect -> potionEffect.getType() == PotionEffectType.ABSORPTION)
            .findFirst()
            .map(PotionEffect::getAmplifier)
            .map(i -> i + 1)
            .orElse(0);

        this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, STRONG_RESTORE.ticks(), tier));
    }

    private void giveAdrenaline() {
        this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, STRENGTH_II_TIME.ticks(), 0));
        this.getPlayer().sendMessage(C.info(C.RED) + "Adrenaline courses through your body");

        if (this.adrenalineFadeMessageTask != null) {
            this.adrenalineFadeMessageTask.cancel();
        }

        this.adrenalineFadeMessageTask = Bukkit.getScheduler().runTaskLater(this.getPlugin(), () -> {
            this.getPlayer().sendMessage(C.info(C.AQUA) + "Your adrenaline fades...");
        }, STRENGTH_II_TIME.ticks());
        this.attach(this.adrenalineFadeMessageTask);
    }

    @EventHandler
    public void onDamagedWhileVulnerable(EntityDamageEvent event) {
        if (event.getEntity() != this.getPlayer()) {
            return;
        }

        if (this.vulnerable) {
            if (IGNORE_WHILE_VULNERABLE.contains(event.getCause())) {
                event.setCancelled(true);
            } else {
                event.setDamage(1000);
            }
        }
    }

    @EventHandler
    public void onDamageWhileVulnerable(EntityDamageByEntityEvent event) {
        if (event.getDamager() != this.getPlayer()) {
            return;
        }

        if (this.vulnerable && !this.strong) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamagePlayerWhileStrong(EntityDamageByEntityEvent event) {
        if (this.strong && event.getDamager() == this.getPlayer() && event.getEntity() instanceof Player damaged) {
            if (damaged.isBlocking()) {
                // End strength early
                if (this.endStrengthTask != null && this.endStrengthTask instanceof Runnable r) {
                    r.run();
                    event.setCancelled(true);
                    this.endStrengthTask.cancel();
                }
                return;
            }

            event.setDamage(1000);

            this.giveAdrenaline();
            this.giveAbsorption();
        }
    }

    @EventHandler
    public void onKillPlayer(PlayerKilledByPlayerEvent event) {
        if (event.getKiller() == this.getPlayer()) {
            if (!this.strong) {
                // Restore redstone
                this.redstone.increment(1);
                this.cooldownManager.storeCooldown(this.getPlayer(), Duration.ZERO);
            }

            this.getPlayer().setHealth(Math.min(this.getPlayer().getMaxHealth(), this.getPlayer().getHealth() + 6));
        }
    }

}
