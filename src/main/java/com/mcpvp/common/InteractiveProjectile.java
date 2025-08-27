package com.mcpvp.common;

import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class InteractiveProjectile implements EasyListener {

    @Getter
    private final Plugin plugin;
    private final Projectile projectile;

    private boolean singleEventOnly;
    private Consumer<Player> hitPlayerConsumer;
    private Consumer<EntityDamageByEntityEvent> damageEventConsumer;
    private Consumer<ProjectileHitEvent> hitEventConsumer;
    private Runnable deathRunnable;

    public InteractiveProjectile singleEventOnly() {
        this.singleEventOnly = true;
        return this;
    }

    public InteractiveProjectile onHitPlayer(Consumer<Player> consumer) {
        this.hitPlayerConsumer = consumer;
        return this;
    }

    public InteractiveProjectile onDamageEvent(Consumer<EntityDamageByEntityEvent> consumer) {
        this.damageEventConsumer = consumer;
        return this;
    }

    public InteractiveProjectile onHitEvent(Consumer<ProjectileHitEvent> consumer) {
        this.hitEventConsumer = consumer;
        return this;
    }

    public InteractiveProjectile onDeath(Runnable runnable) {
        this.deathRunnable = runnable;
        return this;
    }

    @EventHandler
    private void onEntityDamageByProjectile(EntityDamageByEntityEvent event) {
        if (event.getDamager() == projectile) {
            if (event.getEntity() instanceof Player hit && hitPlayerConsumer != null) {
                hitPlayerConsumer.accept(hit);
            } else if (damageEventConsumer != null) {
                damageEventConsumer.accept(event);
            }

            if (singleEventOnly) {
                unregister();
            }
        }
    }

    @EventHandler
    private void onProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntity() == projectile && hitEventConsumer != null) {
            hitEventConsumer.accept(event);

            if (singleEventOnly) {
                unregister();
            }
        }
    }

    @EventHandler
    private void onDeath(EntityDeathEvent event) {
        if (event.getEntity() == projectile) {
            deathRunnable.run();
            unregister();
        }
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (projectile.isDead() || !projectile.isValid()) {
            if (deathRunnable != null) {
                deathRunnable.run();
            }
            unregister();
        }
    }

}
