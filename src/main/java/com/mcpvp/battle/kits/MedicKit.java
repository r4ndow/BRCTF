package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.ProjectileManager;
import com.mcpvp.common.event.EventUtil;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureBuilder;
import com.mcpvp.common.structure.StructureManager;
import com.mcpvp.common.util.BlockUtil;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public class MedicKit extends BattleKit {

    private static final int MAX_WEBS = 3;
    private final StructureManager manager = new StructureManager();

    public MedicKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
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
        KitItem sword = new KitItem(MedicKit.this, ItemBuilder.of(Material.GOLD_SWORD).enchant(Enchantment.DAMAGE_ALL, 1).unbreakable().build());

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

    public void heal(Player player) {
        Kit kit = getBattle().getKitManager().get(player);
        if (kit == null) {
            return;
        }

        player.playEffect(EntityEffect.HURT);

        // Players must be full health before restoring items
        if (player.getHealth() == player.getMaxHealth()) {
            // Restore items
            kit.getAllItems().stream().filter(KitItem::isRestorable).forEach(ki -> {
                this.restoreItem(player, kit, ki);
            });
        } else {
            // Heal player
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 4));
            player.setFireTicks(0);
        }
    }

    private void restoreItem(Player player, Kit kit, KitItem item) {
        // Un-placeholder
        item.increment(item.getOriginal().getAmount());

        // Ensure max amount
        if (item.getItem().getAmount() != item.getOriginal().getAmount()) {
            item.getItem().setAmount(item.getOriginal().getAmount());
        }
    }

    public class MedicWebItem extends KitItem {

        private final ProjectileManager projectileManager;

        public MedicWebItem(ItemStack itemStack) {
            super(MedicKit.this, itemStack);
            this.projectileManager = MedicKit.this.getBattle().getProjectileManager();
            this.onInteract(this::throwWeb);
        }

        public void throwWeb(PlayerInteractEvent event) {
            if (!EventUtil.isRightClick(event))
                return;

            event.setCancelled(true);

            if (isPlaceholder())
                return;

            decrement(true);

            Snowball ent = kit.getPlayer().launchProjectile(Snowball.class);
            projectileManager.register(ent)
                    .onHit(e -> this.placeWeb(ent.getLocation()))
                    .onCollideBlock(e -> this.placeWeb(ent.getLocation()));
        }

        public void placeWeb(Location location) {
            // TODO by truncating this to the block's location, it might lose accuracy
            Block target = location.getBlock();
            if (!target.isEmpty()) {
                Block nearestAir = BlockUtil.getNearestType(target, Material.AIR, 2);
                if (nearestAir != null)
                    target = nearestAir;
            }

            MedicWeb web = new MedicWeb(manager);
            placeStructure(web, target);
        }

    }

    public static class MedicWeb extends Structure {

        public MedicWeb(StructureManager manager) {
            super(manager);
        }

        @Override
        public void build(Block center, StructureBuilder builder) {
            builder.setBlock(center, Material.WEB);
        }

    }

}
