package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.item.CooldownItem;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EffectUtil;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TricksterKit extends BattleKit {

    private static final Duration STOLEN_ITEM_DISPLAY_TIME = Duration.seconds(10);
    private static final Duration STOLEN_ITEM_RETURN_TIME = Duration.seconds(7);

    private static final Duration STEAL_HIT_COOLDOWN = Duration.seconds(3);
    private static final Duration VICTIM_STEAL_PROTECTION_COOLDOWN = Duration.seconds(15);
    private static final Map<UUID, Expiration> VICTIM_STEAL_PROTECTION = new HashMap<>();

    private static final Duration PUMPKIN_HEAD_COOLDOWN = Duration.seconds(10);
    private static final Duration PUMPKIN_HELMET_TIME = Duration.seconds(4);
    private static final Map<UUID, HelmetSwapData> PUMPKIN_SWAPS = new HashMap<>();

    private static final double BLAST_RADIUS = 3.0;
    private static final double BLAST_STRENGTH = 1.15;
    private static final double BLAST_Y = 0.55;

    private final Map<Player, StolenItemData> activeSteals = new HashMap<>();

    private final Duration pumpkinSlowDuration;
    private final Duration pumpkinBlindDuration;
    private final Duration pumpkinNauseaDuration;

    private StealAbility stealAbility;
    private PumpkinHeadAbility pumpkinHeadAbility;

    public TricksterKit(BattlePlugin plugin, Player player) {
        super(plugin, player);

        this.pumpkinSlowDuration = Duration.seconds(
                this.getPlugin().getConfig().getInt("kits.trickster.pumpkin-head.slowness-seconds", 7)
        );
        this.pumpkinBlindDuration = Duration.seconds(
                this.getPlugin().getConfig().getInt("kits.trickster.pumpkin-head.blindness-seconds", 2)
        );
        this.pumpkinNauseaDuration = Duration.seconds(
                this.getPlugin().getConfig().getInt("kits.trickster.pumpkin-head.nausea-seconds", 6)
        );

        this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0), true);
    }

    @Override
    public String getName() {
        return "Trickster";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
                ItemBuilder.of(Material.LEATHER_BOOTS).color(Color.fromRGB(31, 59, 44)).build(),
                ItemBuilder.of(Material.LEATHER_LEGGINGS).color(Color.fromRGB(43, 43, 43)).build(),
                ItemBuilder.of(Material.LEATHER_CHESTPLATE).color(Color.fromRGB(75, 27, 21)).build(),
                ItemBuilder.of(Material.LEATHER_HELMET).color(Color.fromRGB(230, 126, 34)).build()
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        this.stealAbility = new StealAbility();
        this.pumpkinHeadAbility = new PumpkinHeadAbility();

        return new KitInventoryBuilder()
                .add(ItemBuilder.of(Material.WOOD_SWORD)
                        .name("Trickster Sword")
                        .enchant(Enchantment.DAMAGE_ALL, 1)
                        .enchant(Enchantment.KNOCKBACK, 2)
                        .unbreakable())
                .addFood(5)
                .add(this.stealAbility)
                .add(this.pumpkinHeadAbility)
                .addCompass(8)
                .build();
    }

    @EventHandler(ignoreCancelled = true)
    public void onHitWithStealAbility(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (attacker != this.getPlayer()) return;
        if (this.stealAbility == null) return;

        if (!this.stealAbility.isItem(this.getPlayer().getItemInHand())) return;
        if (this.getGame().getTeamManager().isSameTeam(attacker, victim)) return;

        if (this.stealAbility.isOnHitCooldown()) {
            event.setCancelled(true);
            this.getPlayer().sendMessage(C.RED + "Steal is on cooldown!");
            return;
        }

        if (attemptSteal(victim)) {
            this.stealAbility.startHitCooldown();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHitWithPumpkinHead(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (attacker != this.getPlayer()) return;
        if (this.pumpkinHeadAbility == null) return;

        if (!this.pumpkinHeadAbility.isItem(this.getPlayer().getItemInHand())) return;
        if (this.getGame().getTeamManager().isSameTeam(attacker, victim)) return;
        if (this.pumpkinHeadAbility.isOnHitCooldown()) return;

        applyPumpkinHelmet(victim);
        applyPumpkinEffects(victim);
        spawnPumpkinFirework(victim);

        this.pumpkinHeadAbility.startHitCooldown();
    }

    private void spawnPumpkinFirework(Player victim) {
        FireworkEffect effect = FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL)
                .withColor(Color.fromRGB(230, 126, 34))
                .build();

        EffectUtil.sendInstantFirework(effect, victim.getLocation());
    }

    private void applyPumpkinEffects(Player victim) {
        PotionEffect slow = new PotionEffect(PotionEffectType.SLOW, this.pumpkinSlowDuration.toTicks(), 0);
        PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, this.pumpkinBlindDuration.toTicks(), 0);
        PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, this.pumpkinNauseaDuration.toTicks(), 0);

        Optional<BattleKit> victimKit = this.getBattle().getKitManager().find(victim);
        if (victimKit.isPresent()) {
            victimKit.get().addTemporaryEffect(slow);
            victimKit.get().addTemporaryEffect(blind);
            victimKit.get().addTemporaryEffect(nausea);
        } else {
            victim.addPotionEffect(slow, true);
            victim.addPotionEffect(blind, true);
            victim.addPotionEffect(nausea, true);
        }
    }

    private void applyPumpkinHelmet(Player victim) {
        ItemStack original = victim.getInventory().getHelmet();
        UUID token = UUID.randomUUID();

        PUMPKIN_SWAPS.put(
                victim.getUniqueId(),
                new HelmetSwapData(token, original == null ? null : original.clone())
        );

        victim.getInventory().setHelmet(new ItemStack(Material.PUMPKIN));
        victim.updateInventory();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!victim.isOnline()) {
                    PUMPKIN_SWAPS.remove(victim.getUniqueId());
                    return;
                }

                HelmetSwapData data = PUMPKIN_SWAPS.get(victim.getUniqueId());
                if (data == null || !data.getToken().equals(token)) return;

                ItemStack current = victim.getInventory().getHelmet();
                if (current != null && current.getType() == Material.PUMPKIN) {
                    victim.getInventory().setHelmet(data.getOriginalHelmet());
                    victim.updateInventory();
                }

                PUMPKIN_SWAPS.remove(victim.getUniqueId());
            }
        }.runTaskLater(this.getPlugin(), PUMPKIN_HELMET_TIME.toTicks());
    }

    private boolean attemptSteal(Player victim) {
        Player thief = this.getPlayer();
        ItemStack held = victim.getItemInHand();

        if (held == null || held.getType() == Material.AIR) {
            thief.sendMessage(C.RED + "Target has no item to steal!");
            return false;
        }

        if (isNonStealableItem(held)) {
            thief.sendMessage(C.RED + "You cannot steal that item!");
            return false;
        }

        Expiration protection = VICTIM_STEAL_PROTECTION.get(victim.getUniqueId());
        if (protection != null && !protection.isExpired()) {
            long secondsLeft = Math.max(0, protection.getRemaining().toSeconds());
            thief.sendMessage(C.RED + victim.getName() + " cannot be stolen from for " + secondsLeft + " seconds!");
            return false;
        }

        int freeSlot = thief.getInventory().firstEmpty();
        if (freeSlot == -1) {
            thief.sendMessage(C.RED + "Your inventory is full!");
            return false;
        }

        ItemStack placeholder = ItemBuilder.of(Material.STAINED_GLASS_PANE)
                .name(C.GRAY + "Stolen!")
                .data((short) 14)
                .build();

        int victimSlot = victim.getInventory().getHeldItemSlot();

        victim.getInventory().setItem(victimSlot, placeholder);
        victim.updateInventory();

        ItemStack stolenCopy = held.clone();
        thief.getInventory().setItem(freeSlot, stolenCopy);
        thief.updateInventory();

        StolenItemData data = new StolenItemData(victim, thief, held.clone(), victimSlot, freeSlot);
        this.activeSteals.put(victim, data);

        thief.sendMessage(C.GREEN + "You stole " + victim.getName() + "'s item!");
        victim.sendMessage(C.RED + thief.getName() + " stole your item!");
        thief.playSound(thief.getLocation(), Sound.ITEM_PICKUP, 1.0f, 1.0f);
        victim.playSound(victim.getLocation(), Sound.ITEM_PICKUP, 1.0f, 1.0f);

        this.attach(new BukkitRunnable() {
            @Override
            public void run() {
                StolenItemData current = activeSteals.get(victim);
                if (current == null) return;

                Player t = current.getThief();
                if (t == null || !t.isOnline()) return;

                ItemStack inSlot = t.getInventory().getItem(current.getThiefSlot());
                if (inSlot != null && inSlot.isSimilar(current.getOriginalItem())) {
                    t.getInventory().setItem(current.getThiefSlot(), null);
                    t.updateInventory();
                }
            }
        }.runTaskLater(this.getPlugin(), STOLEN_ITEM_DISPLAY_TIME.toTicks()));

        this.attach(new BukkitRunnable() {
            @Override
            public void run() {
                returnStolenItem(victim);
            }
        }.runTaskLater(this.getPlugin(), STOLEN_ITEM_RETURN_TIME.toTicks()));

        VICTIM_STEAL_PROTECTION.put(victim.getUniqueId(), Expiration.after(VICTIM_STEAL_PROTECTION_COOLDOWN));
        return true;
    }

    private boolean isNonStealableItem(ItemStack item) {
        if (item == null) return false;

        Material type = item.getType();
        if (type == Material.STAINED_GLASS_PANE) return true;
        if (type == Material.COMPASS) return true;

        return this.getGame().getTeamManager().getTeams().stream()
                .anyMatch(team -> team.getFlag().isItem(item));
    }

    private void returnStolenItem(Player victim) {
        StolenItemData data = this.activeSteals.remove(victim);
        if (data == null) return;

        Player thief = data.getThief();
        if (thief != null && thief.isOnline()) {
            ItemStack inSlot = thief.getInventory().getItem(data.getThiefSlot());

            if (inSlot != null && inSlot.isSimilar(data.getOriginalItem())) {
                thief.getInventory().setItem(data.getThiefSlot(), null);
            } else {
                for (int i = 0; i < thief.getInventory().getSize(); i++) {
                    ItemStack stack = thief.getInventory().getItem(i);
                    if (stack != null && stack.isSimilar(data.getOriginalItem())) {
                        thief.getInventory().setItem(i, null);
                        break;
                    }
                }
            }
            thief.updateInventory();
        }

        if (victim.isOnline()) {
            victim.getInventory().setItem(data.getVictimSlot(), data.getOriginalItem());
            victim.updateInventory();
            victim.playSound(victim.getLocation(), Sound.ITEM_PICKUP, 1.0f, 0.1f);
            victim.sendMessage(C.GREEN + "Your stolen item has been returned!");
        }

        if (this.getPlayer().isOnline()) {
            this.getPlayer().sendMessage(C.GRAY + "Stolen item returned to " + victim.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPumpkinHeadPlace(BlockPlaceEvent event) {
        if (this.pumpkinHeadAbility == null) return;
        if (event.getPlayer() != this.getPlayer()) return;

        // Only trigger for the kit item being placed
        if (event.getBlockPlaced().getType() != Material.PUMPKIN) return;
        if (!this.pumpkinHeadAbility.isItem(event.getItemInHand())) return;

        // Detect attempt, don't allow real building
        event.setCancelled(true);

        if (this.pumpkinHeadAbility.isPlaceholder()) return;
        if (this.pumpkinHeadAbility.isOnHitCooldown()) return;

        Location blast = event.getBlockPlaced().getLocation().add(0.5, 0.5, 0.5);
        this.blast(blast);
        this.pumpkinHeadAbility.startHitCooldown();
    }

    public void blast(Location blast) {
        blast.getWorld().playSound(blast, Sound.EXPLODE, 1.0f, 1.0f);
        blast.getWorld().playSound(blast, Sound.FIRE_IGNITE, 0.6f, 1.6f);

        for (Player nearby : blast.getWorld().getPlayers()) {
            if (nearby.getLocation().distance(blast) > BLAST_RADIUS) continue;

            Vector knock = nearby.getLocation().toVector().subtract(blast.toVector());
            if (knock.lengthSquared() > 0) {
                knock.normalize().multiply(BLAST_STRENGTH);
            } else {
                knock = new Vector(0, 0, 0);
            }

            knock.setY(BLAST_Y);
            nearby.setVelocity(knock);
        }
    }






    @Override
    public void shutdown() {
        for (Player victim : new HashMap<>(this.activeSteals).keySet()) {
            returnStolenItem(victim);
        }
        super.shutdown();
    }

    private class StealAbility extends CooldownItem {

        private final Expiration hitCooldown = new Expiration();

        public StealAbility() {
            super(
                    TricksterKit.this,
                    ItemBuilder.of(Material.STICK)
                            .name("Steal")
                            .desc(C.GRAY + "Hit an enemy to steal their held item.", 40)
                            .build(),
                    STEAL_HIT_COOLDOWN
            );
        }

        @Override
        protected boolean autoUse() {
            return false;
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
            // Intentionally unused (hit-triggered ability).
        }

        @Override
        protected void onFailedUse() {
            TricksterKit.this.getPlayer().sendMessage(C.RED + "Steal is on cooldown!");
        }

        public boolean isOnHitCooldown() {
            return !this.hitCooldown.isExpired();
        }

        public void startHitCooldown() {
            this.hitCooldown.expireIn(STEAL_HIT_COOLDOWN);
            this.setPlaceholder();
            this.startCooldown();
        }

        public void showStolenAppearance(ItemStack victimItem) {
            this.modify(builder -> {
                builder.type(victimItem.getType());
                builder.durability(victimItem.getDurability());

                if (victimItem.hasItemMeta() && victimItem.getItemMeta().hasDisplayName()) {
                    builder.rawName(victimItem.getItemMeta().getDisplayName());
                } else {
                    builder.name(victimItem.getType().toString());
                }

                victimItem.getEnchantments().forEach(builder::enchant);
                return builder;
            });
        }

        public void clearStolenAppearance() {
            this.restore();
        }
    }

    private class PumpkinHeadAbility extends CooldownItem {

        private static final double BLAST_RADIUS = 3.0;
        private static final double BLAST_STRENGTH = 1.15;
        private static final double BLAST_Y = 0.55;

        private final Expiration hitCooldown = new Expiration();

        public PumpkinHeadAbility() {
            super(
                    TricksterKit.this,
                    ItemBuilder.of(Material.PUMPKIN)
                            .name("Pumpkin Head")
                            .desc(C.GRAY + "Hit an enemy to pumpkin their head.", 40)
                            .build(),
                    PUMPKIN_HEAD_COOLDOWN
            );
        }

        @Override
        protected boolean autoUse() {
            return false;
        }

        @Override
        protected boolean shouldTrigger(PlayerInteractEvent event) {
            return EventUtil.isRightClick(event);
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (event.getClickedBlock() == null || event.getBlockFace() == null) return;

            event.setCancelled(true);

            if (this.isPlaceholder()) return;

            if (!this.hitCooldown.isExpired()) {
                TricksterKit.this.getPlayer().sendMessage(C.RED + "Pumpkin Head is on cooldown!");
                return;
            }

            Location blast = event.getClickedBlock().getRelative(event.getBlockFace())
                    .getLocation().add(0.5, 0.5, 0.5);

            blast.getWorld().playSound(blast, Sound.EXPLODE, 1.0f, 1.0f);
            blast.getWorld().playSound(blast, Sound.FIRE_IGNITE, 0.6f, 1.6f);

            for (Player nearby : blast.getWorld().getPlayers()) {
                if (nearby.getLocation().distance(blast) > BLAST_RADIUS) continue;

                Vector knock = nearby.getLocation().toVector().subtract(blast.toVector());
                if (knock.lengthSquared() > 0) {
                    knock.normalize().multiply(BLAST_STRENGTH);
                } else {
                    knock = new Vector(0, 0, 0);
                }

                knock.setY(BLAST_Y);
                nearby.setVelocity(knock);
            }

            startHitCooldown();
        }

        public boolean isOnHitCooldown() {
            return !this.hitCooldown.isExpired();
        }

        public void startHitCooldown() {
            this.hitCooldown.expireIn(PUMPKIN_HEAD_COOLDOWN);
            this.setPlaceholder();
            this.startCooldown();
        }
    }

    @Getter
    private static class StolenItemData {
        private final Player victim;
        private final Player thief;
        private final ItemStack originalItem;
        private final int victimSlot;
        private final int thiefSlot;

        public StolenItemData(Player victim, Player thief, ItemStack originalItem, int victimSlot, int thiefSlot) {
            this.victim = victim;
            this.thief = thief;
            this.originalItem = originalItem;
            this.victimSlot = victimSlot;
            this.thiefSlot = thiefSlot;
        }
    }

    @Getter
    private static class HelmetSwapData {
        private final UUID token;
        private final ItemStack originalHelmet;

        public HelmetSwapData(UUID token, ItemStack originalHelmet) {
            this.token = token;
            this.originalHelmet = originalHelmet;
        }
    }
}
