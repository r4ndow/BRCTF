package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kits.global.NecroRevivalTagManager;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EffectUtil;
import com.mcpvp.common.util.EntityUtil;
import com.mcpvp.common.util.PlayerUtil;
import com.mcpvp.common.util.chat.C;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EntityWitherSkull;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class NecroKit extends BattleKit {

    private static final Duration TAG_COOLDOWN = Duration.secs(15);
    private static final Duration TAG_DURATION = Duration.secs(10);

    private static final int SKULL_EFFECT_RADIUS = 4;
    private static final Duration SKULL_ABSORPTION_DURATION = Duration.secs(30);
    private static final Duration SKULL_REGENERATION_TIME = Duration.secs(20);

    private final NecroRevivalTagManager revivalTagManager;
    private KitItem tag;

    public NecroKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
        this.revivalTagManager = plugin.getBattle().getKitManager().getNecroRevivalTagManager();
    }

    @Override
    public String getName() {
        return "Necro";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            new ItemStack(Material.IRON_BOOTS),
            new ItemStack(Material.IRON_LEGGINGS),
            new ItemStack(Material.GOLD_CHESTPLATE),
            new ItemStack(Material.IRON_HELMET)
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        tag = new KitItem(this, ItemBuilder.of(Material.NAME_TAG).name("Revival Tag").build());
        tag.onInteractEntity(event -> {
           if (event.getRightClicked() instanceof Player clicked) {
               this.attemptRevivalTag(clicked);
           }
        });

        return new KitInventoryBuilder()
            .add(ItemBuilder.of(Material.GOLD_SWORD).name("Necro Sword").enchant(Enchantment.DAMAGE_ALL, 2).unbreakable())
            .addFood(4)
            .add(tag)
            .add(new CrypticSkull())
            .build();
    }

    private void attemptRevivalTag(Player target) {
        if (!isTeammate(target) || tag.isPlaceholder()) {
            return;
        }

        if (revivalTagManager.isRevivalTagged(target)) {
            getPlayer().sendMessage(C.warn(C.GOLD) + C.hl(target.getName()) + " is already revival tagged!");
            return;
        }

        if (revivalTagManager.isZombie(target)) {
            getPlayer().sendMessage(C.warn(C.GOLD) + C.hl(target.getName()) + " is already a zombie!");
            return;
        }

        revivalTagManager.setRevivalTagged(target);
        getPlayer().sendMessage(C.info(C.AQUA) + "You revival tagged " + C.hl(target.getName()) + "!");
        target.sendMessage(
            C.info(C.GREEN) + C.hl(getPlayer().getName())
                + " has revival tagged you. You are safe for the next "
                + C.hl("" + TAG_DURATION.seconds()) + " seconds!");

        attach(EasyTask.of(() -> tag.restore()).runTaskLater(getPlugin(), TAG_COOLDOWN.ticks()));
    }

    private WitherSkull spawnWitherSkull() {
        Vector vector = getPlayer().getEyeLocation().getDirection().clone().normalize().multiply(0.1D);
        EntityPlayer entityPlayer = PlayerUtil.asCraftPlayer(getPlayer());
        EntityWitherSkull witherSkull = new EntityWitherSkull(
            entityPlayer.getWorld(),
            entityPlayer,
            vector.getX(),
            vector.getY(),
            vector.getZ()
        );
        witherSkull.dirX = vector.getX();
        witherSkull.dirY = vector.getY();
        witherSkull.dirZ = vector.getZ();
        witherSkull.locX = getPlayer().getEyeLocation().getX();
        witherSkull.locY = getPlayer().getEyeLocation().getY();
        witherSkull.locZ = getPlayer().getEyeLocation().getZ();
        entityPlayer.getWorld().addEntity(witherSkull);

        WitherSkull bukkitEntity = (WitherSkull) witherSkull.getBukkitEntity();
        bukkitEntity.setShooter(getPlayer());

        return bukkitEntity;
    }

    private void explodeSkull(WitherSkull skull) {
        // Negative particles
        ParticlePacket wither = ParticlePacket.blockDust(Material.OBSIDIAN)
            .at(skull.getLocation())
            .spread(SKULL_EFFECT_RADIUS / 2)
            .count(SKULL_EFFECT_RADIUS * 10);
        getEnemies().forEach(wither::send);

        // Positive particles
        ParticlePacket absorption = ParticlePacket.blockDust(Material.GOLD_BLOCK)
            .at(skull.getLocation())
            .spread(SKULL_EFFECT_RADIUS / 2)
            .count(SKULL_EFFECT_RADIUS * 10);
        getTeammates().forEach(absorption::send);

        // Impact nearby players
        EntityUtil.getNearbyEntities(skull.getLocation(), Player.class, SKULL_EFFECT_RADIUS).stream()
            .filter(getGame()::isParticipant)
            .filter(player -> !getGame().getTeamManager().getTeam(player).isInSpawn(player))
            .forEach(player -> {
                if (isTeammate(player)) {
                    onSkullHitsTeammate(player);
                } else {
                    onSkullHitsEnemy(player);
                }
            });

        // Remove nearby webs
        getBattle().getStructureManager().getStructures().stream()
            .filter(structure -> structure instanceof MedicKit.MedicWeb)
            .filter(structure -> isEnemy(structure.getOwner()))
            .filter(structure -> structure.distance(skull.getLocation()) <= SKULL_EFFECT_RADIUS)
            .forEach(Structure::remove);

        skull.remove();
    }

    private void onSkullHitsTeammate(Player player) {
        int amplifier = player.hasPotionEffect(PotionEffectType.ABSORPTION) ? 1 : 0;
        player.addPotionEffect(
            new PotionEffect(PotionEffectType.ABSORPTION, SKULL_ABSORPTION_DURATION.ticks(), amplifier, true), true
        );

        player.sendMessage(C.info(C.AQUA) + C.hl(getPlayer().getName()) + " gave you absorption!");
    }

    private void onSkullHitsEnemy(Player player) {
        player.sendMessage(C.warn(C.GOLD) + C.hl(getPlayer().getName()) + " withered you!");
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, Duration.seconds(5).ticks(), 2, true));
    }

    public class CrypticSkull extends KitItem {

        private final Queue<Expiration> restores = new LinkedList<>();

        public CrypticSkull() {
            super(
                NecroKit.this,
                ItemBuilder.of(Material.SKULL_ITEM).name("Cryptic Skull").data(1).build()
            );

            onInteract(event -> launch());
        }

        private void launch() {
            if (isPlaceholder() || inSpawn()) {
                return;
            }

            // Projectile launch
            WitherSkull witherSkull = spawnWitherSkull();

            // Handling
            attach(
                new InteractiveProjectile(getPlugin(), witherSkull)
                    .onHitEvent(event -> explodeSkull(witherSkull))
            );

            // Effects
            attach(EffectUtil.colorTrail(witherSkull, getTeam().getColor().getColor()).runTaskTimer(getPlugin(), 0, 1));
            getPlayer().getWorld().playSound(getPlayer().getEyeLocation(), Sound.WITHER_SHOOT, 1.0f, 1.0f);

            // Item handling
            decrement(true);

            Expiration expiration = new Expiration().expireIn(SKULL_REGENERATION_TIME);
            restores.add(expiration);
            attach(EasyTask.of(this::regenerate).runTaskLater(getPlugin(), SKULL_REGENERATION_TIME.ticks()));
        }

        private void regenerate() {
            if (!restores.isEmpty()) {
                restores.remove();
            }
            increment(3);
        }

        @EventHandler
        public void updateExp(TickEvent event) {
            if (isItem(getPlayer().getItemInHand()) && !restores.isEmpty()) {
                getPlayer().setExp(restores.peek().getCompletionPercent(SKULL_REGENERATION_TIME));
            }
        }

        @EventHandler
        public void onKillOtherPlayer(PlayerKilledByPlayerEvent event) {
            if (event.getKiller() == getPlayer()) {
                regenerate();
            }
        }

    }

}
