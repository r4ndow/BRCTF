package com.mcpvp.battle.kits;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerKilledByPlayerEvent;
import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.battle.kit.item.CooldownItem;
import com.mcpvp.battle.kits.global.NecroRevivalTagManager;
import com.mcpvp.battle.kits.global.ScoutDeathTagManager;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.InteractiveProjectile;
import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.task.EasyTask;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import com.mcpvp.common.util.EntityUtil;
import com.mcpvp.common.util.LocationUtil;
import com.mcpvp.common.util.PlayerUtil;
import com.mcpvp.common.chat.C;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class ScoutKit extends BattleKit {

    private static final Duration SWAPPER_COOLDOWN_OTHERS = Duration.seconds(10);
    private static final Duration SWAPPER_ITEM_COOLDOWN = Duration.seconds(1.5);
    private static final Duration TAG_COOLDOWN = Duration.seconds(15);
    private static final Map<Player, Expiration> COOLDOWN_MAP = new HashMap<>();

    private final ScoutDeathTagManager deathTagManager;
    private final NecroRevivalTagManager revivalTagManager;
    private KitItem swapper;
    private DeathTagItem deathTagItem;

    public ScoutKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
        this.deathTagManager = this.getBattle().getKitManager().getScoutDeathTagManager();
        this.revivalTagManager = this.getBattle().getKitManager().getNecroRevivalTagManager();

        this.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0));
    }

    @Override
    public String getName() {
        return "Scout";
    }

    @Override
    public ItemStack[] createArmor() {
        return new ItemStack[]{
            ItemBuilder.of(Material.IRON_BOOTS).enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1).build(),
            null,
            null,
            new ItemStack(Material.IRON_HELMET)
        };
    }

    @Override
    public Map<Integer, KitItem> createItems() {
        return new KitInventoryBuilder()
            .add(ItemBuilder.of(Material.STONE_SWORD)
                .name("Scout Sword")
                .enchant(Enchantment.DAMAGE_ALL, 2)
                .unbreakable())
            .addFood(2)
            .add(this.swapper = new SwapperItem())
            .add(this.deathTagItem = new DeathTagItem())
            .addCompass(8)
            .build();
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() == this.getPlayer() && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setDamage(event.getDamage() * 0.3);
        }
    }

    @EventHandler
    public void onKillOtherPlayer(PlayerKilledByPlayerEvent event) {
        if (event.getKiller() == this.getPlayer() && !this.deathTagManager.isDeathTagged(event.getKilled())) {
            this.deathTagItem.restore();
        }
    }

    private void throwSwapperBall() {
        this.swapper.setPlaceholder();

        Snowball snowball = this.getPlayer().launchProjectile(Snowball.class);
        double velocity = snowball.getVelocity().multiply(1.5).length();
        snowball.setVelocity(this.getPlayer().getEyeLocation().getDirection().multiply(velocity));

        this.attach(new InteractiveProjectile(this.getPlugin(), snowball)
            .onDamageEvent(event -> {
                event.setCancelled(true);
                if (event.getEntity() instanceof Player hit) {
                    this.attemptSwap(hit);
                }
            })
        );
    }

    private void attemptSwap(Player player) {
        if (!this.isEnemy(player)) {
            return;
        }

        if (this.isOnSwapCooldown(player)) {
            return;
        }

        this.swap(player);
        this.swapper.restore();
    }

    private void swap(Player player) {
        Location toKitPlayer = player.getLocation();
        Location toSwappedPlayer = this.getPlayer().getLocation();

        this.getPlayer().teleport(toKitPlayer);
        player.teleport(toSwappedPlayer);

        LocationUtil.trace(
            this.getPlayer().getLocation(),
            player.getLocation(),
            (int) this.getPlayer().getLocation().distance(player.getLocation()) * 2
        ).forEach(loc -> {
            ParticlePacket.of(EnumParticle.SMOKE_NORMAL).at(loc).send();
        });

        this.getPlayer().playSound(this.getPlayer().getEyeLocation(), Sound.ENDERMAN_TELEPORT, 0.5f, 0.5f);
        player.playSound(player.getEyeLocation(), Sound.ENDERMAN_TELEPORT, 0.5f, 0.5f);

        PlayerUtil.setAbsorptionHearts(
            this.getPlayer(), Math.min(PlayerUtil.getAbsorptionHearts(this.getPlayer()) + 4, 8)
        );

        this.startSwapCooldown(player);
        this.sendSwapNotification(player);
    }

    private void sendSwapNotification(Player swapped) {
        int distance = (int) swapped.getLocation().distance(this.getPlayer().getLocation());
        this.getPlayer().sendMessage("You swapped with " + C.hl(swapped.getName()) + " from " + distance + " blocks");
        swapped.sendMessage(C.hl(this.getPlayer().getName()) + " swapped with you from " + distance + " blocks");

        String nearby = this.getGame().findClosestCallout(swapped.getLocation()).map(callout ->
            C.GRAY + " (near " + callout.getText() + C.GRAY + ")"
        ).orElse("");

        if (this.getGame().getTeamManager().getTeams().stream().noneMatch(bt ->
            bt.getFlag().getCarrier() == swapped
        )) {
            return;
        }

        EntityUtil.getNearbyEntities(swapped.getLocation(), Player.class, 20).forEach(player -> {
            if (player != swapped) {
                if (this.getGame().getTeamManager().isSameTeam(player, swapped)) {
                    player.sendMessage(C.warn(C.GOLD) + "Your flag carrier was swapped!");
                } else if (this.getGame().isParticipant(player)) {
                    player.sendMessage(C.warn(C.GOLD) + "The enemy flag carrier was swapped! " + nearby);
                } else {
                    player.sendMessage(C.warn(C.GOLD) + "The "
                        + this.getGame().getTeamManager().getTeam(swapped).getColoredName()
                        + " flag carrier was swapped! " + nearby);
                }
            }
        });
    }

    private boolean isOnSwapCooldown(Player player) {
        return COOLDOWN_MAP.containsKey(player) && !COOLDOWN_MAP.get(player).isExpired();
    }

    private void startSwapCooldown(Player player) {
        Expiration expiration = COOLDOWN_MAP.computeIfAbsent(player, p -> new Expiration());
        expiration.expireIn(SWAPPER_COOLDOWN_OTHERS);

        // This should not be attached to the kit, as it is a global cooldown
        EasyTask.of(() -> {
            player.sendMessage(C.info(C.AQUA) + "You can now be swapped again");
        }).runTaskLater(this.getPlugin(), expiration.getRemaining().ticks());
    }

    private boolean attemptDeathTag(Player player) {
        if (!this.isEnemy(player) || this.deathTagItem.isPlaceholder()) {
            return false;
        }

        return this.deathTag(player);
    }

    private boolean deathTag(Player player) {
        if (this.revivalTagManager.isRevivalTagged(player)) {
            this.revivalTagManager.clearRevivalTag(player);
            player.sendMessage(C.info(C.GRAY) + "Your revival tag was removed by the death tag of " + C.hl(this.getPlayer().getName()));
            this.getPlayer().sendMessage(C.warn(C.GOLD) + "You removed the revival tag on " + C.hl(player.getName()));
            return false;
        }

        boolean tagged = this.deathTagManager.setDeathTagged(player);
        if (tagged) {
            this.getPlayer().sendMessage(C.info(C.GOLD) + "You have death tagged " + C.hl(player.getName()));

            this.getGame().getTeamManager().getTeams().stream()
                .filter(bt -> bt.getFlag().getCarrier() == player)
                .findAny()
                .ifPresent(this::sendCarrierDeathTagMessages);
        } else {
            this.getPlayer().sendMessage(C.warn(C.GOLD) + C.hl(player.getName()) + " is already death tagged!");
        }

        return tagged;
    }

    private void sendCarrierDeathTagMessages(BattleTeam team) {
        team.getPlayers().forEach(enemy ->
            enemy.sendMessage(C.warn(C.GOLD) + "You flag carrier was death tagged!")
        );

        this.getTeammates().forEach(teammate ->
            teammate.sendMessage(C.warn(C.GOLD) + "The enemy flag carrier was death tagged!")
        );

        this.getGame().getSpectators().forEach(spectator ->
            spectator.sendMessage(C.warn(C.GOLD) + "The " + team.getColoredName() + " flag carrier was death tagged!")
        );
    }

    class SwapperItem extends CooldownItem {

        public SwapperItem() {
            super(
                ScoutKit.this,
                ItemBuilder.of(Material.SLIME_BALL).name("Swapper").build(),
                SWAPPER_ITEM_COOLDOWN
            );
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
            if (!this.isPlaceholder()) {
                ScoutKit.this.throwSwapperBall();
            }
        }

    }

    class DeathTagItem extends CooldownItem {

        public DeathTagItem() {
            super(
                ScoutKit.this,
                ItemBuilder.of(Material.NAME_TAG).name("Death Tag").build(),
                TAG_COOLDOWN
            );

            this.onInteractEntity(event -> {
                if (event.getRightClicked() instanceof Player hit) {
                    if (ScoutKit.this.attemptDeathTag(hit)) {
                        this.startCooldown();
                        this.setPlaceholder();
                    }
                }
            });
            this.onDamage(event -> {
                if (event.getEntity() instanceof Player hit) {
                    if (ScoutKit.this.attemptDeathTag(hit)) {
                        this.startCooldown();
                        this.setPlaceholder();
                    }
                }
            });
        }

        @Override
        protected void onUse(PlayerInteractEvent event) {
        }

        @Override
        protected boolean autoUse() {
            return false;
        }

    }

}
