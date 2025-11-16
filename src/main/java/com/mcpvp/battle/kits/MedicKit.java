package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagPoisonEvent;
import com.mcpvp.battle.hud.impl.HealthHeadIndicator;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EasyEvent;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.nms.ActionbarUtil;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureBuilder;
import com.mcpvp.common.structure.StructureManager;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.BlockUtil;
import lombok.Data;
import org.bukkit.Color;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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

import static com.mcpvp.battle.match.BattleMatchStructureRestrictions.*;

public class MedicKit extends BattleKit {

    private static final Duration WEB_RESTORE_TIMER = Duration.seconds(5);
    private static final Duration RESTORE_HEALTH_TIMER = Duration.seconds(1);
    private static final Duration RESTORE_PLAYER_COOLDOWN = Duration.seconds(15);
    private static final Duration COMBAT_COOLDOWN = Duration.seconds(5);
    private static final int MAX_WEBS = 3;
    private static final Map<Player, Expiration> HEAL_COOLDOWNS = new HashMap<>();

    private final Expiration combatCooldown = new Expiration();

    public MedicKit(BattlePlugin plugin, Player player) {
        super(plugin, player);

        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 99999, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 99999, 1));

        this.attach(new HealthHeadIndicator(plugin, player));
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
            ItemBuilder.of(Material.GOLD_SWORD)
                .name("Medic Sword")
                .enchant(Enchantment.DAMAGE_ALL, 2)
                .unbreakable()
                .build()
        );

        sword.onDamage(ev -> {
            if (ev.getEntity() instanceof Player damaged) {
                BattleTeam damagedTeam = this.getBattle().getGame().getTeamManager().getTeam(damaged);
                BattleTeam playerTeam = this.getBattle().getGame().getTeamManager().getTeam(this.getPlayer());

                if (damagedTeam == playerTeam) {
                    this.heal(damaged);
                }
            }
        });

        return new KitInventoryBuilder()
            .add(sword)
            .addFood(6)
            .add(new MedicWebItem(ItemBuilder.of(Material.SNOW_BALL)
                .name("Medic Web")
                .amount(MAX_WEBS)
                .build()))
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (event.isInterval(RESTORE_HEALTH_TIMER)) {
            this.getPlayer().setHealth(Math.min(this.getPlayer().getMaxHealth(), this.getPlayer().getHealth() + 1));
        }
        this.getPlayer().setFireTicks(0);

        if (event.isInterval(Duration.seconds(0.5))) {
            this.getTeammates().stream()
                .filter(teammate -> teammate.getLocation().distance(this.getPlayer().getLocation()) <= 10)
                .forEach(teammate -> {
                    Color color;
                    if (teammate.hasPotionEffect(PotionEffectType.REGENERATION)) {
                        // Probably being healed
                        color = Color.ORANGE;
                    } else if (this.needsItems(teammate)) {
                        color = Color.RED;
                    } else {
                        color = Color.GREEN;
                    }

                    ParticlePacket.colored(color)
                        .at(teammate.getLocation().add(0, 2.02, 0))
                        .setShowFar(false)
                        .send(this.getPlayer());
                });
        }
    }

    @EventHandler
    public void preventFlagPoison(FlagPoisonEvent event) {
        if (event.getPlayer() == this.getPlayer()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true) // friendly fire events will be cancelled
    public void triggerCombatCooldown(EntityDamageByEntityEvent event) {
        if (event.getEntity().equals(this.getPlayer()) && event.getDamager() instanceof Player) {
            this.combatCooldown.expireIn(COMBAT_COOLDOWN);
        }
    }

    public void heal(Player player) {
        Kit kit = this.getBattle().getKitManager().get(player);
        if (kit == null) {
            return;
        }

        // Enforce a global cooldown per player
        if (HEAL_COOLDOWNS.containsKey(player) && !HEAL_COOLDOWNS.get(player).isExpired()) {
            String duration = HEAL_COOLDOWNS.get(player).getRemaining().formatText();
            ActionbarUtil.send(this.getPlayer(), "%s can't be healed for %s second(s)".formatted(C.hl(player.getName()), C.hl(duration)));
            ActionbarUtil.send(player, "%sYou can't be healed for %s second(s)".formatted(C.GRAY, C.hl(duration)));
            return;
        }

        // Enforce a cooldown specifically on other medics
        if (kit instanceof MedicKit otherMedic) {
            if (!this.combatCooldown.isExpired()) {
                ActionbarUtil.send(this.getPlayer(), "%sYou can't heal another medic for %s seconds(s)".formatted(C.GRAY, C.hl(this.combatCooldown.getRemaining().formatText())));
                return;
            }

            if (!otherMedic.combatCooldown.isExpired()) {
                ActionbarUtil.send(this.getPlayer(), "%s can't be healed by another medic for %s seconds(s)".formatted(C.hl(player.getName()), C.hl(this.combatCooldown.getRemaining().formatText())));
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
            HEAL_COOLDOWNS.put(player, Expiration.after(RESTORE_PLAYER_COOLDOWN));

            new HealEvent(player).call();
        } else {
            // Heal player
            this.getBattle().getKitManager().find(player).ifPresent(playerKit -> {
                playerKit.addTemporaryEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 4));
            });
            player.setFireTicks(0);
        }
    }

    private boolean needsItems(Player player) {
        BattleKit kit = this.getBattle().getKitManager().get(player);
        if (kit == null) {
            return false;
        }

        for (KitItem kitItem : kit.getAllItems()) {
            if (!kitItem.isRestorable()) {
                continue;
            }

            for (ItemStack content : player.getInventory().getContents()) {
                if (kitItem.isItem(content) && !kitItem.getOriginal().equals(content)) {
                    return true;
                }
            }
        }

        return false;
    }

    public class MedicWebItem extends KitItem {

        public MedicWebItem(ItemStack itemStack) {
            super(MedicKit.this, itemStack, true);
            this.onInteract(this::throwWeb);
        }

        public void throwWeb(PlayerInteractEvent event) {
            if (!EventUtil.isRightClick(event)) {
                return;
            }

            event.setCancelled(true);

            if (this.isPlaceholder()) {
                return;
            }

            this.decrement(true);

            Snowball snowball = this.kit.getPlayer().launchProjectile(Snowball.class);
            MedicKit.this.attach(new InteractiveProjectile(this.getPlugin(), snowball)
                .singleEventOnly()
                .onDamageEvent(this::onHitEvent)
                .onHitEvent(e -> this.placeWeb(snowball.getLocation()))
            );
        }

        private void onHitEvent(EntityDamageByEntityEvent event) {
            if (!(event.getEntity() instanceof Player hit)) {
                return;
            }

            if (!(event.getDamager() instanceof Projectile proj) || !(proj.getShooter() instanceof Player shooter)) {
                return;
            }

            if (MedicKit.this.getGame().getTeamManager().isSameTeam(hit, shooter)) {
                event.setCancelled(true);
            } else {
                this.placeWeb(hit.getLocation());
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

            MedicWeb web = new MedicWeb(MedicKit.this.getBattle().getStructureManager());
            MedicKit.this.placeStructure(web, target);
        }

        @EventHandler
        public void restorePassively(TickEvent event) {
            if (event.isInterval(WEB_RESTORE_TIMER)) {
                this.increment(this.getOriginal().getAmount());
            }
        }

    }

    public class MedicWeb extends Structure {

        public MedicWeb(StructureManager manager) {
            super(manager, MedicKit.this.getPlayer());
            this.removeAfter(Duration.seconds(3));
        }

        @Override
        public void build(Block center, StructureBuilder builder) {
            builder.ignoreRestrictions(
                NEAR_SPAWN, NEAR_RESTRICTED, NEAR_PLAYER
            );
            builder.setBlock(center, Material.WEB);
        }

        @Override
        public Plugin getPlugin() {
            return MedicKit.this.getPlugin();
        }

        @EventHandler
        public void onInteract(PlayerInteractEvent event) {
            boolean sameTeam = MedicKit.this.getBattle().getGame().getTeamManager().getTeam(event.getPlayer()).contains(MedicKit.this.getPlayer());
            if (this.getBlocks().contains(event.getClickedBlock()) && sameTeam && !EventUtil.isRightClick(event)) {
                this.remove();
            }
        }

    }

    @Data
    public static class HealEvent extends EasyEvent {
        private final Player healed;
    }

}
