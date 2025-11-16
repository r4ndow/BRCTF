package com.mcpvp.common;

import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
        if (event.getDamager() == this.projectile) {
            if (event.getEntity() instanceof Player hit && this.hitPlayerConsumer != null) {
                this.hitPlayerConsumer.accept(hit);
                if (this.singleEventOnly) {
                    this.unregister();
                }
            } else if (this.damageEventConsumer != null) {
                this.damageEventConsumer.accept(event);
                if (this.singleEventOnly) {
                    this.unregister();
                }
            }
        }
    }

    @EventHandler
    private void onProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntity() == this.projectile && this.hitEventConsumer != null) {
            this.hitEventConsumer.accept(event);

            if (this.singleEventOnly) {
                this.unregister();
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent event) {
        // EntityDeathEvent is not fired for a Projectile, so check manually
        if (this.projectile.isDead() || !this.projectile.isValid()) {
            if (this.deathRunnable != null) {
                this.deathRunnable.run();
            }
            this.unregister();
        }
    }

}
