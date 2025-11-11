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
import com.mcpvp.common.chat.C;
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
        this.tag = new KitItem(this, ItemBuilder.of(Material.NAME_TAG).name("Revival Tag").build());
        this.tag.onInteractEntity(event -> {
            if (event.getRightClicked() instanceof Player clicked) {
                this.attemptRevivalTag(clicked);
            }
        });

        return new KitInventoryBuilder()
            .add(ItemBuilder.of(Material.GOLD_SWORD)
                .name("Necro Sword")
                .enchant(Enchantment.DAMAGE_ALL, 2)
                .unbreakable())
            .addFood(4)
            .add(this.tag)
            .add(new CrypticSkull())
            .build();
    }

    private void attemptRevivalTag(Player target) {
        if (!this.isTeammate(target) || this.tag.isPlaceholder()) {
            return;
        }

        if (this.revivalTagManager.isRevivalTagged(target)) {
            this.getPlayer().sendMessage(C.warn(C.GOLD) + C.hl(target.getName()) + " is already revival tagged!");
            return;
        }

        if (this.revivalTagManager.isZombie(target)) {
            this.getPlayer().sendMessage(C.warn(C.GOLD) + C.hl(target.getName()) + " is already a zombie!");
            return;
        }

        this.revivalTagManager.setRevivalTagged(target);
        this.getPlayer().sendMessage(C.info(C.AQUA) + "You revival tagged " + C.hl(target.getName()) + "!");
        target.sendMessage(
            C.info(C.GREEN) + C.hl(this.getPlayer().getName())
                + " has revival tagged you. You are safe for the next "
                + C.hl("" + TAG_DURATION.seconds()) + " seconds!");

        this.tag.setPlaceholder();
        this.attach(EasyTask.of(() -> this.tag.restore()).runTaskLater(this.getPlugin(), TAG_COOLDOWN.ticks()));
    }

    private WitherSkull spawnWitherSkull() {
        // There is a known bug here. WitherSkulls have a built-in acceleration which causes them to not
        // be properly reflected by Elf shields. There's no (easy) way to control it in this version.
        Vector vector = this.getPlayer().getEyeLocation().getDirection().clone().normalize().multiply(0.1D);
        EntityPlayer entityPlayer = PlayerUtil.asCraftPlayer(this.getPlayer());
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
        witherSkull.locX = this.getPlayer().getEyeLocation().getX();
        witherSkull.locY = this.getPlayer().getEyeLocation().getY();
        witherSkull.locZ = this.getPlayer().getEyeLocation().getZ();
        entityPlayer.getWorld().addEntity(witherSkull);

        WitherSkull bukkitEntity = (WitherSkull) witherSkull.getBukkitEntity();
        bukkitEntity.setShooter(this.getPlayer());

        return bukkitEntity;
    }

    private void explodeSkull(WitherSkull skull) {
        // The shooter might change due to Elf reflection
        if (!(skull.getShooter() instanceof Player shooter)) {
            return;
        }

        ParticlePacket absorptionParticle = ParticlePacket.blockDust(Material.GOLD_BLOCK)
            .at(skull.getLocation())
            .spread(SKULL_EFFECT_RADIUS / 2)
            .count(SKULL_EFFECT_RADIUS * 10);

        ParticlePacket witherParticle = ParticlePacket.blockDust(Material.OBSIDIAN)
            .at(skull.getLocation())
            .spread(SKULL_EFFECT_RADIUS / 2)
            .count(SKULL_EFFECT_RADIUS * 10);

        // Impact nearby players
        this.getGame().getParticipants().forEach(player -> {
            if (this.getGame().getTeamManager().getTeam(player).isInSpawn(player)) {
                return;
            }

            boolean teammate = this.getGame().getTeamManager().isSameTeam(shooter, player);
            if (teammate) {
                absorptionParticle.send(player);
                if (player.getLocation().distance(skull.getLocation()) <= SKULL_EFFECT_RADIUS) {
                    this.onSkullHitsTeammate(player);
                }
            } else {
                witherParticle.send(player);
                if (player.getLocation().distance(skull.getLocation()) <= SKULL_EFFECT_RADIUS) {
                    this.onSkullHitsEnemy(player);
                }
            }
        });

        // Remove nearby webs
        this.getBattle().getStructureManager().getStructures().stream()
            .filter(structure -> structure instanceof MedicKit.MedicWeb)
            .filter(structure -> !this.getGame().getTeamManager().isSameTeam(shooter, structure.getOwner()))
            .filter(structure -> structure.distance(skull.getLocation()) <= SKULL_EFFECT_RADIUS)
            .forEach(Structure::remove);

        skull.remove();
    }

    private void onSkullHitsTeammate(Player player) {
        int amplifier = player.hasPotionEffect(PotionEffectType.ABSORPTION) ? 1 : 0;
        player.addPotionEffect(
            new PotionEffect(PotionEffectType.ABSORPTION, SKULL_ABSORPTION_DURATION.ticks(), amplifier, true), true
        );

        player.sendMessage(C.info(C.AQUA) + C.hl(this.getPlayer().getName()) + " gave you absorption!");
    }

    private void onSkullHitsEnemy(Player player) {
        player.sendMessage(C.warn(C.GOLD) + C.hl(this.getPlayer().getName()) + " withered you!");
        player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, Duration.seconds(5).ticks(), 2, true));
    }

    public class CrypticSkull extends KitItem {

        private final Queue<Expiration> restores = new LinkedList<>();

        public CrypticSkull() {
            super(
                NecroKit.this,
                ItemBuilder.of(Material.SKULL_ITEM).name("Cryptic Skull").data(1).build()
            );

            this.onInteract(event -> this.launch());
        }

        private void launch() {
            if (this.isPlaceholder() || NecroKit.this.inSpawn()) {
                return;
            }

            // Projectile launch
            WitherSkull witherSkull = NecroKit.this.spawnWitherSkull();

            // Handling
            NecroKit.this.attach(
                new InteractiveProjectile(this.getPlugin(), witherSkull)
                    .onHitEvent(event -> NecroKit.this.explodeSkull(witherSkull))
            );

            // Effects
            NecroKit.this.attach(EffectUtil.colorTrail(witherSkull, NecroKit.this.getTeam().getColor().getColor()).runTaskTimer(this.getPlugin(), 0, 1));
            NecroKit.this.getPlayer().getWorld().playSound(NecroKit.this.getPlayer().getEyeLocation(), Sound.WITHER_SHOOT, 1.0f, 1.0f);

            // Item handling
            this.decrement(true);

            Expiration expiration = Expiration.after(SKULL_REGENERATION_TIME);
            this.restores.add(expiration);
            NecroKit.this.attach(EasyTask.of(this::regenerate).runTaskLater(this.getPlugin(), SKULL_REGENERATION_TIME.ticks()));
        }

        private void regenerate() {
            if (!this.restores.isEmpty()) {
                this.restores.remove();
            }
            this.increment(3);
        }

        @EventHandler
        public void updateExp(TickEvent event) {
            if (this.isItem(NecroKit.this.getPlayer().getItemInHand()) && !this.restores.isEmpty()) {
                NecroKit.this.getPlayer().setExp(this.restores.peek().getCompletionPercent(SKULL_REGENERATION_TIME));
            }
        }

        @EventHandler
        public void onKillOtherPlayer(PlayerKilledByPlayerEvent event) {
            if (event.getKiller() == NecroKit.this.getPlayer()) {
                this.regenerate();
            }
        }

    }

}
