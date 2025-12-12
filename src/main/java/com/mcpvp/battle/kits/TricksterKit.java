package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.item.CooldownItem;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureBuilder;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EffectUtil;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
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
    private static final Duration STEAL_COOLDOWN = Duration.seconds(3);
    private static final Duration VICTIM_STEAL_COOLDOWN = Duration.seconds(15);
    private static final Map<UUID, Expiration> VICTIM_STEAL_PROTECTION = new HashMap<>();

    private static final Duration PUMPKIN_HEAD_COOLDOWN = Duration.seconds(10);
    private static final Duration PUMPKIN_HELMET_TIME = Duration.seconds(4);

    private static final Duration PUMPKINPLACETIME = Duration.seconds(4);


    private static final Map<UUID, HelmetSwapData> PUMPKIN_SWAPS = new HashMap<>();

    private final Map<Player, StolenItemData> stolenItems = new HashMap<>();

    private final Expiration pumpkinHeadCooldown = new Expiration();

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
                this.getPlugin().getConfig().getInt("kits.trickster.pumpkin-head.nausea-seconds", 8)
        );

        this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0));
    }

    @Override
    public String getName() {
        return "Trickster";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
                ItemBuilder.of(Material.LEATHER_BOOTS)
                        .color(Color.fromRGB(31, 59, 44))
                        //.enchant(Enchantment.PROTECTION_FALL, 3)
                        .build(),
                ItemBuilder.of(Material.LEATHER_LEGGINGS)
                        .color(Color.fromRGB(43, 43, 43))
                        .build(),
                ItemBuilder.of(Material.LEATHER_CHESTPLATE)
                        .color(Color.fromRGB(75, 27, 90))
                        .build(),
                ItemBuilder.of(Material.LEATHER_HELMET)
                        .color(Color.fromRGB(230, 126, 34))
                        .build()
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
        if (!this.stealAbility.isItem(this.getPlayer().getItemInHand())) return;

        if (this.stealAbility.isOnHitCooldown()) {
            event.setCancelled(true);
            this.getPlayer().sendMessage(C.warn(C.RED) + "Steal is on cooldown!");
            return;
        }

        if (this.getBattle().getGame().getTeamManager().isSameTeam(attacker, victim)) {
            return;
        }

        if (this.attemptSteal(victim)) {
            this.stealAbility.startHitCooldown();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHitPumpkinHead(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (attacker != this.getPlayer()) return;

        if (this.pumpkinHeadAbility == null) return;
        if (!this.pumpkinHeadAbility.isItem(this.getPlayer().getItemInHand())) return;

        if (this.getBattle().getGame().getTeamManager().isSameTeam(attacker, victim)) {
            return;
        }

        if (this.pumpkinHeadAbility.isOnHitCooldown()) {
            return;
        }

        this.applyPumpkinHelmet(victim);
        this.applyPumpkinEffects(victim);
        this.spawnPumpkinFirework(victim);

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
        PotionEffect slow = new PotionEffect(PotionEffectType.SLOW, this.pumpkinSlowDuration.ticks(), 0);
        PotionEffect blind = new PotionEffect(PotionEffectType.BLINDNESS, this.pumpkinBlindDuration.ticks(), 0);
        PotionEffect nausea = new PotionEffect(PotionEffectType.CONFUSION, this.pumpkinNauseaDuration.ticks(), 0);

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
                if (data == null || !data.getToken().equals(token)) {
                    return;
                }

                ItemStack current = victim.getInventory().getHelmet();
                if (current != null && current.getType() == Material.PUMPKIN) {
                    victim.getInventory().setHelmet(data.getOriginalHelmet());
                    victim.updateInventory();
                }

                PUMPKIN_SWAPS.remove(victim.getUniqueId());
            }
        }.runTaskLater(this.getPlugin(), PUMPKIN_HELMET_TIME.ticks());
    }

    private boolean attemptSteal(Player victim) {
        Player thief = this.getPlayer();
        ItemStack heldItem = victim.getItemInHand();

        if (heldItem == null || heldItem.getType() == Material.AIR) {
            thief.sendMessage(C.warn(C.RED) + "Target has no item to steal!");
            return false;
        }

        if (isNonStealableItem(heldItem)) {
            thief.sendMessage(C.warn(C.RED) + "You cannot steal that item");
            return false;
        }

        Expiration protection = VICTIM_STEAL_PROTECTION.get(victim.getUniqueId());
        if (protection != null && !protection.isExpired()) {
            long secondsLeft = Math.max(0, protection.getRemaining().seconds());
            thief.sendMessage(
                    C.warn(C.RED) + C.hl(victim.getName()) + " cannot be stolen from for " + secondsLeft + " seconds!"
            );
            return false;
        }

        int freeSlot = thief.getInventory().firstEmpty();
        if (freeSlot == -1) {
            thief.sendMessage(C.warn(C.RED) + "Your inventory is full! You cannot steal");
            return false;
        }

        ItemStack placeholder = ItemBuilder.of(Material.STAINED_GLASS_PANE)
                .name(C.GRAY + "Stolen!")
                .data((short) 14)
                .build();

        int victimSlot = victim.getInventory().getHeldItemSlot();
        victim.getInventory().setItem(victimSlot, placeholder);
        victim.updateInventory();

        ItemStack stolenCopy = heldItem.clone();
        thief.getInventory().setItem(freeSlot, stolenCopy);
        thief.updateInventory();

        StolenItemData data = new StolenItemData(
                victim,
                thief,
                heldItem.clone(),
                victimSlot,
                freeSlot
        );

        this.stolenItems.put(victim, data);

        thief.sendMessage(C.info(C.GREEN) + "You stole " + C.hl(victim.getName()) + "'s item!");
        victim.sendMessage(C.warn(C.RED) + C.hl(thief.getName()) + " stole your item!");
        thief.playSound(thief.getLocation(), Sound.ITEM_PICKUP, 1.0f, 1.0f);
        victim.playSound(victim.getLocation(), Sound.ITEM_PICKUP, 1.0f, 1.0f);

        this.attach(new BukkitRunnable() {
            @Override
            public void run() {
                StolenItemData current = stolenItems.get(victim);
                if (current == null) {
                    return;
                }

                Player t = current.getThief();
                if (t == null || !t.isOnline()) {
                    return;
                }

                ItemStack inSlot = t.getInventory().getItem(current.getThiefSlot());
                if (inSlot != null && inSlot.isSimilar(current.getOriginalItem())) {
                    t.getInventory().setItem(current.getThiefSlot(), null);
                    t.updateInventory();
                }
            }
        }.runTaskLater(this.getPlugin(), STOLEN_ITEM_DISPLAY_TIME.ticks()));

        this.attach(new BukkitRunnable() {
            @Override
            public void run() {
                returnStolenItem(victim);
            }
        }.runTaskLater(this.getPlugin(), STOLEN_ITEM_RETURN_TIME.ticks()));

        VICTIM_STEAL_PROTECTION.put(
                victim.getUniqueId(),
                Expiration.after(VICTIM_STEAL_COOLDOWN)
        );

        return true;
    }

    private boolean isNonStealableItem(ItemStack item) {
        if (item == null) {
            return false;
        }

        if (item.getType() == Material.STAINED_GLASS_PANE) {
            return true;
        }

        if (item.getType() == Material.COMPASS) {
            return true;
        }

        return this.getBattle().getGame().getTeamManager().getTeams().stream()
                .anyMatch(team -> team.getFlag().isItem(item));
    }

    private void returnStolenItem(Player victim) {
        StolenItemData data = this.stolenItems.remove(victim);
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
            victim.sendMessage(C.info(C.GREEN) + "Your stolen item has been returned!");
        }

        if (this.getPlayer().isOnline()) {
            this.getPlayer().sendMessage(
                    C.info(C.GRAY) + "Stolen item returned to " + victim.getName()
            );
        }
    }

    private class PumpkinBlockStructure extends Structure {
        public PumpkinBlockStructure() {
            super(TricksterKit.this.getBattle().getStructureManager(), TricksterKit.this.getPlayer());
            this.removeAfter(PUMPKINPLACETIME);
        }

        @Override
        protected void build(Block center, StructureBuilder builder) {
            builder.setBlock(center, Material.PUMPKIN);
        }

        @Override
        public Plugin getPlugin() {
            return TricksterKit.this.getPlugin();
        }
    }


    @Override
    public void shutdown() {
        for (Player victim : new HashMap<>(this.stolenItems).keySet()) {
            returnStolenItem(victim);
        }

        super.shutdown();
    }



    class StealAbility extends CooldownItem {

        private final Expiration hitCooldown = new Expiration();

        public StealAbility() {
            super(
                    TricksterKit.this,
                    ItemBuilder.of(Material.STICK)
                            .name("Steal")
                            .desc(C.GRAY + "Hit an enemy to steal their held item.", 40)
                            .build(),
                    STEAL_COOLDOWN
            );
        }

        @Override
        protected boolean autoUse() {
            return false;
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
        }

        @Override
        protected void onFailedUse() {
            TricksterKit.this.getPlayer().sendMessage(
                    C.warn(C.RED) + "Steal is on cooldown!"
            );
        }

        public boolean isOnHitCooldown() {
            return !this.hitCooldown.isExpired();
        }

        public void startHitCooldown() {
            this.hitCooldown.expireIn(STEAL_COOLDOWN);
            this.setPlaceholder();
            this.startCooldown();
        }

        public void showStolenAppearance(ItemStack victimItem) {
            this.modify(builder -> {
                builder
                        .type(victimItem.getType())
                        .durability(victimItem.getDurability());

                if (victimItem.hasItemMeta() && victimItem.getItemMeta().hasDisplayName()) {
                    builder.rawName(victimItem.getItemMeta().getDisplayName());
                } else {
                    builder.name(victimItem.getType().toString());
                }

                victimItem.getEnchantments().forEach(
                        (ench, level) -> builder.enchant(ench, level)
                );

                return builder;
            });
        }

        public void clearStolenAppearance() {
            this.restore();
        }
    }

    class PumpkinHeadAbility extends CooldownItem {

        private final Expiration hitCooldown = new Expiration();

        public PumpkinHeadAbility() {
            super(
                    TricksterKit.this,
                    ItemBuilder.ofMaterial(Material.PUMPKIN)
                            .name("Pumpkin Head")
                            .desc(C.GRAY + "Right-click a block to place a temporary pumpkin.", 40)
                            .build(),
                    PUMPKINHEADCOOLDOWN
            );
        }

        @Override
        protected boolean autoUse() {
            return false; // we will handle interact manually
        }

        @EventHandler
        public void onInteract(PlayerInteractEvent event) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (!this.isItem(event.getItem())) return;

            // keep vanilla placement from happening + keep behavior consistent
            event.setCancelled(true);

            if (TricksterKit.this.inSpawn()) return;
            if (this.isPlaceholder()) return;

            if (!this.hitCooldown.isExpired()) {
                TricksterKit.this.getPlayer.sendMessage(C.warn + C.RED + "Pumpkin Head is on cooldown!");
                return;
            }

            Block clicked = event.getClickedBlock();
            if (clicked == null) return;

            Block target = clicked.getRelative(event.getBlockFace());
            if (!target.isEmpty()) return; // don't overwrite existing blocks

            boolean placed = TricksterKit.this.placeStructure(new PumpkinBlockStructure(), target);
            if (!placed) return;

            // start cooldown only after success
            this.hitCooldown.expireIn(PUMPKINHEAD_COOLDOWN);
            this.setPlaceholder();
            this.startCooldown();
        }

        public boolean isOnHitCooldown() {
            return !this.hitCooldown.isExpired();
        }

        public void startHitCooldown() {
            // unchanged: this is used by the on-hit ability
            this.hitCooldown.expireIn(PUMPKINHEAD_COOLDOWN);
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

        public StolenItemData(Player victim,
                              Player thief,
                              ItemStack originalItem,
                              int victimSlot,
                              int thiefSlot) {
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
