package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.task.DrainExpBarTask;
import com.mcpvp.common.task.FillExpBarTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.chat.C;
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

    private boolean strong;
    private boolean vulnerable;
    private KitItem redstone;
    private KitItem sugar;
    private BukkitTask adrenalineFadeMessageTask;

    public AssassinKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
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
        KitItem sword = new KitItem(this, ItemBuilder.of(Material.GOLD_SWORD)
            .name("Assassin Sword")
            .enchant(Enchantment.DAMAGE_ALL, 2)
            .unbreakable()
            .build()
        );
        redstone = new KitItem(this, ItemBuilder.of(Material.REDSTONE).name("Assassinate").build());
        sugar = new KitItem(this, ItemBuilder.of(Material.SUGAR).name("Speed Boost").amount(SUGAR_AMOUNT).build());

        redstone.onInteract(event -> {
            if (EventUtil.isRightClick(event)) {
                activateStrength();
            }
        });

        sugar.onInteract(event -> {
            if (EventUtil.isRightClick(event)) {
                activateSpeed();
            }
        });

        return new KitInventoryBuilder()
            .add(sword)
            .add(redstone)
            .add(sugar)
            .addCompass(8)
            .build();
    }

    private void activateStrength() {
        // Swap to the sword slot
        int slot = getPlayer().getInventory().first(Material.IRON_SWORD);
        if (slot < 9 && slot > -1) {
            getPlayer().getInventory().setHeldItemSlot(slot);
        }

        // Activate
        getPlayer().sendMessage(C.info(C.AQUA) + "You are now in " + C.hl("Assassin mode") + "!");
        strong = true;
        vulnerable = true;
        redstone.decrement(true);

        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, VULNERABLE_TIME.ticks(), 0));
        getPlayer().setExp(1);

        // Empty XP bar
        animateExp(new DrainExpBarTask(getPlayer(), STRONG_TIME));

        // Task to restore the redstone
        attach(Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            redstone.increment(1);
        }, STRONG_TIME.add(STRONG_RESTORE).ticks()));

        // Task to end the strength
        attach(Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            getPlayer().sendMessage(C.info(C.AQUA) + "Your strength fades...");
            strong = false;
            animateExp(new FillExpBarTask(getPlayer(), STRONG_RESTORE));
        }, STRONG_TIME.ticks()));

        // Task to end vulnerability
        attach(Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            getPlayer().sendMessage(C.info(C.AQUA) + "You are no longer vulnerable");
            vulnerable = false;
        }, VULNERABLE_TIME.ticks()));
    }

    private void activateSpeed() {
        sugar.decrement(true);

        // Add speed effect
        PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, SPEEDY_TIME.ticks(), 2);
        getPlayer().addPotionEffect(effect, true);

        // Restore the item
        attach(Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            sugar.increment(SUGAR_AMOUNT);
        }, SPEEDY_RESTORE.ticks()));
    }

    private void giveAbsorption() {
        int tier = getPlayer().getActivePotionEffects().stream()
            .filter(potionEffect -> potionEffect.getType() == PotionEffectType.ABSORPTION)
            .findFirst()
            .map(PotionEffect::getAmplifier)
            .map(i -> i + 1)
            .orElse(0);

        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, STRONG_RESTORE.ticks(), tier));
    }

    private void giveAdrenaline() {
        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, STRENGTH_II_TIME.ticks(), 0));
        getPlayer().sendMessage(C.info(C.RED) + "Adrenaline courses through your body");

        if (adrenalineFadeMessageTask != null) {
            adrenalineFadeMessageTask.cancel();
        }

        adrenalineFadeMessageTask = Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            getPlayer().sendMessage(C.info(C.AQUA) + "Your adrenaline fades...");
        }, STRENGTH_II_TIME.ticks());
        attach(adrenalineFadeMessageTask);
    }

    @EventHandler
    public void onDamagedWhileVulnerable(EntityDamageEvent event) {
        if (event.getEntity() != getPlayer()) {
            return;
        }

        if (vulnerable) {
            if (IGNORE_WHILE_VULNERABLE.contains(event.getCause())) {
                event.setCancelled(true);
            } else {
                event.setDamage(1000);
            }
        }
    }

    @EventHandler
    public void onDamagePlayerWhileStrong(EntityDamageByEntityEvent event) {
        if (strong && event.getDamager() == getPlayer() && event.getEntity() instanceof Player damaged) {
            if (damaged.isBlocking()) {
                return;
            }

            event.setDamage(1000);

            giveAdrenaline();
            giveAbsorption();
        }
    }

    @EventHandler
    public void onKillPlayer(PlayerKilledByPlayerEvent event) {
        if (event.getKiller() == getPlayer()) {
            if (!strong) {
                // Restore redstone
                redstone.increment(1);
            }

            getPlayer().setHealth(Math.min(getPlayer().getMaxHealth(), getPlayer().getHealth() + 6));
        }
    }

}
