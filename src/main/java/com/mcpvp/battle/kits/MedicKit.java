package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagPoisonEvent;
import com.mcpvp.battle.hud.impl.HealthHeadIndicator;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.ProjectileManager;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureBuilder;
import com.mcpvp.common.structure.StructureManager;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.BlockUtil;
import com.mcpvp.common.util.chat.C;
import com.mcpvp.common.util.nms.ActionbarUtil;
import lombok.NonNull;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class MedicKit extends BattleKit {

    private static final Duration WEB_RESTORE_TIMER = Duration.seconds(15);
    private static final Duration RESTORE_HEALTH_TIMER = Duration.seconds(1);
    private static final Duration RESTORE_PLAYER_COOLDOWN = Duration.seconds(15);
    private static final Duration COMBAT_COOLDOWN = Duration.seconds(5);
    private static final int MAX_WEBS = 64;
    private static final Map<Player, Expiration> HEAL_COOLDOWNS = new HashMap<>();

    private final Expiration combatCooldown = new Expiration();

    public MedicKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected void setup(@NonNull Player player) {
        super.setup(player);

        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 99999, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 99999, 1));

        attach(new HealthHeadIndicator(plugin, player));
    }

    @Override
    public String getName() {
        return "Medic";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
                new ItemStack(Material.GOLD_BOOTS),
                new ItemStack(Material.GOLD_LEGGINGS),
                new ItemStack(Material.GOLD_CHESTPLATE),
                new ItemStack(Material.GOLD_HELMET)
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        KitItem sword = new KitItem(
                this,
                ItemBuilder.of(Material.GOLD_SWORD).name("Medic Sword").enchant(Enchantment.DAMAGE_ALL, 1).unbreakable().build()
        );

        sword.onDamage(ev -> {
            if (ev.getEntity() instanceof Player damaged) {
                BattleTeam damagedTeam = getBattle().getGame().getTeamManager().getTeam(damaged);
                BattleTeam playerTeam = getBattle().getGame().getTeamManager().getTeam(getPlayer());

                if (damagedTeam == playerTeam) {
                    heal(damaged);
                }
            }
        });

        return new KitInventoryBuilder()
                .add(sword)
                .addFood(6)
                .add(new MedicWebItem(ItemBuilder.of(Material.SNOW_BALL).name("Medic Web").amount(MAX_WEBS).build()))
                .addCompass(8)
                .build();
    }

    @EventHandler
    public void regenerateHealth(TickEvent event) {
        if (event.isInterval(RESTORE_HEALTH_TIMER)) {
            getPlayer().setHealth(Math.min(getPlayer().getMaxHealth(), getPlayer().getHealth() + 1));
        }

        getPlayer().setFireTicks(0);
    }

    @EventHandler
    public void preventFlagPoison(FlagPoisonEvent event) {
        if (event.getPlayer() == getPlayer()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true) // friendly fire events will be cancelled
    public void triggerCombatCooldown(EntityDamageByEntityEvent event) {
        if (event.getEntity().equals(getPlayer()) && event.getDamager() instanceof Player) {
            combatCooldown.expireIn(COMBAT_COOLDOWN);
        }
    }

    public void heal(Player player) {
        Kit kit = getBattle().getKitManager().get(player);
        if (kit == null) {
            return;
        }

        // Enforce a global cooldown per player
        if (HEAL_COOLDOWNS.containsKey(player) && !HEAL_COOLDOWNS.get(player).isExpired()) {
            String duration = HEAL_COOLDOWNS.get(player).getRemaining().formatText();
            ActionbarUtil.send(getPlayer(), "%s can't be healed for %s second(s)".formatted(C.hl(player.getName()), C.hl(duration)));
            ActionbarUtil.send(player, "%sYou can't be healed for %s second(s)".formatted(C.GRAY, C.hl(duration)));
            return;
        }

        // Enforce a cooldown specifically on other medics
        if (kit instanceof MedicKit otherMedic) {
            if (!combatCooldown.isExpired()) {
                ActionbarUtil.send(getPlayer(), "%sYou can't heal another medic for %s seconds(s)".formatted(C.GRAY, C.hl(combatCooldown.getRemaining().formatText())));
                return;
            }

            if (!otherMedic.combatCooldown.isExpired()) {
                ActionbarUtil.send(getPlayer(), "%s can't be healed by another medic for %s seconds(s)".formatted(C.hl(player.getName()), C.hl(combatCooldown.getRemaining().formatText())));
                return;
            }
        }

        player.playEffect(EntityEffect.HURT);

        // Players must be full health before restoring items
        if (player.getHealth() == player.getMaxHealth()) {
            // Restore items
            kit.getAllItems().stream()
                    .filter(KitItem::isRestorable)
                    .forEach(KitItem::restore);

            // No healing for a while
            HEAL_COOLDOWNS.put(player, new Expiration().expireIn(RESTORE_PLAYER_COOLDOWN));
        } else {
            // Heal player
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 4));
            player.setFireTicks(0);
        }
    }

    public class MedicWebItem extends KitItem {

        private final ProjectileManager projectileManager;

        public MedicWebItem(ItemStack itemStack) {
            super(MedicKit.this, itemStack, true);
            this.projectileManager = MedicKit.this.getBattle().getProjectileManager();
            this.onInteract(this::throwWeb);
        }

        public void throwWeb(PlayerInteractEvent event) {
            if (!EventUtil.isRightClick(event)) {
                return;
            }

            event.setCancelled(true);

            if (isPlaceholder()) {
                return;
            }

            decrement(true);

            Snowball ent = kit.getPlayer().launchProjectile(Snowball.class);
            projectileManager.register(ent)
                    .onHitEvent(this::onHitEvent)
                    .onCollideBlock(e -> this.placeWeb(ent.getLocation()));
        }

        private void onHitEvent(EntityDamageByEntityEvent event) {
            if (!(event.getEntity() instanceof Player hit)) {
                return;
            }

            boolean sameTeam = getBattle().getGame().getTeamManager().getTeam(hit).contains(getPlayer());
            if (sameTeam) {
                event.setCancelled(true);
            } else {
                placeWeb(hit.getLocation());
            }
        }

        public void placeWeb(Location location) {
            // TODO by truncating this to the block's location, it might lose accuracy
            Block target = location.getBlock();

            ParticlePacket.colored(Color.RED)
                    .at(location)
                    .count(5)
                    .send();

            Optional<Block> nearestAir = BlockUtil.getBlocksInRadius(target, 2).stream()
                    .filter(b -> {
                        return !Stream.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN).allMatch(bf -> {
                            return b.getRelative(bf).getType() == Material.AIR;
                        });
                    })
                    .filter(b -> b.getType() == Material.AIR || b.getType().isTransparent())
                    .min(Comparator.comparingDouble(b -> {
                        return b.getLocation().add(0.5, 0.5, 0.5).distanceSquared(location);
                    }));

            if (nearestAir.isPresent()) {
                target = nearestAir.get();
            } else {
                return;
            }

            MedicWeb web = new MedicWeb(getBattle().getStructureManager());
            placeStructure(web, target);
        }

        @EventHandler
        public void restorePassively(TickEvent event) {
            if (event.isInterval(WEB_RESTORE_TIMER)) {
                increment(getOriginal().getAmount());
            }
        }

    }

    public class MedicWeb extends Structure {

        public MedicWeb(StructureManager manager) {
            super(manager, getPlayer());
            removeAfter(Duration.seconds(2));
        }

        @Override
        public void build(Block center, StructureBuilder builder) {
            builder.setBlock(center, Material.WEB);
        }

        @Override
        public Plugin getPlugin() {
            return MedicKit.this.getPlugin();
        }

        @EventHandler
        public void onInteract(PlayerInteractEvent event) {
            boolean sameTeam = getBattle().getGame().getTeamManager().getTeam(event.getPlayer()).contains(getPlayer());
            if (getBlocks().contains(event.getClickedBlock()) && sameTeam && !EventUtil.isRightClick(event)) {
                this.remove();
            }
        }

    }

}
