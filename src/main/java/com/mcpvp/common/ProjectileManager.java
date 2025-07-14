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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ProjectileManager implements EasyListener {

    @Getter
    private final Plugin plugin;
    private final Map<Projectile, ProjectileListener> projectiles = new HashMap<>();

    /**
     * Spawns a Projectile from the player.
     *
     * @param clazz  The Projectile class to spawn.
     * @param player The player to launch the projectile from.
     * @param speed  The speed of the projectile.
     * @return The Projectile launched.
     */
    public <T extends Projectile> T spawn(Class<T> clazz, Player player, double speed) {
        return player.launchProjectile(clazz, player.getLocation().getDirection().multiply(speed));
    }

    /**
     * Registers the given Projectile for tracking.
     *
     * @param projectile The Projectile to track.
     * @return The ProjectileListener responsible for handling the events of the
     * projectile. This object should be modified for handling the projectile.
     */
    public ProjectileListener register(Projectile projectile) {
        ProjectileListener listener = new ProjectileListener();
        projectiles.put(projectile, listener);
        return listener;
    }

    @EventHandler
    public void checkAlive(TickEvent event) {
        Iterator<Entry<Projectile, ProjectileListener>> it = projectiles.entrySet().iterator();

        while (it.hasNext()) {
            Entry<Projectile, ProjectileListener> set = it.next();
            Projectile ent = set.getKey();

            if (ent.isDead()) {
                set.getValue().triggerMiss();
                it.remove();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHitByProjectile(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Projectile))
            return;

        Projectile ball = (Projectile) event.getDamager();
        Player hit = null;

        if (event.getEntity() instanceof Player)
            hit = (Player) event.getEntity();
        else if (event.getEntity().getPassenger() instanceof Player)
            hit = (Player) event.getEntity().getPassenger();
        if (!projectiles.containsKey(ball))
            return;

        ProjectileListener list = projectiles.remove(ball);
        list.triggerHitEvent(event);
        if (hit != null)
            list.triggerHit(hit);
    }

    @EventHandler
    public void onCollideBlock(ProjectileHitEvent event) {
        Projectile p = event.getEntity();

        if (!projectiles.containsKey(p))
            return;

        projectiles.get(p).blockCollideHandler.accept(event);
    }

    public static class ProjectileListener {

        private Consumer<Player> hitHandler = p -> {
        };
        private Runnable missHandler = () -> {
        };
        private Consumer<EntityDamageByEntityEvent> eventHandler = e -> {
        };
        private Consumer<ProjectileHitEvent> blockCollideHandler = e -> {
        };

        /**
         * Registers a Consumer that will receive a Player object if a player
         * is hit by the projectile.
         *
         * @param hitHandler The Consumer that will handle the Player hit.
         * @return The instance for chaining.
         */
        public ProjectileListener onHit(Consumer<Player> hitHandler) {
            this.hitHandler = hitHandler;
            return this;
        }

        /**
         * Registers a Consumer that will receive a damage event when any
         * entity is hit by the projectile.
         *
         * @param eventHandler The consumer that will handle the event.
         * @return The instance for chaining.
         */
        public ProjectileListener onHitEvent(Consumer<EntityDamageByEntityEvent> eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        /**
         * Registers a Runnable that will be called if the Projectiles misses,
         * e.g. lands in a block or despawns.
         *
         * @param missHandler The Runnable that will be run.
         * @return The instance for chaining.
         */
        public ProjectileListener onMiss(Runnable missHandler) {
            this.missHandler = missHandler;
            return this;
        }

        // /**
        //  * Registers a Consumer that will receive a Entity.
        //  *
        //  * @param collideHandler The consumer that will handle the event.
        //  * @return The instance for chaining.
        //  */
        // public ProjectileListener onCollideEntity(Consumer<Entity> collideHandler) {
        //     this.entityCollideHandler = collideHandler;
        //     return this;
        // }

        public ProjectileListener onCollideBlock(Consumer<ProjectileHitEvent> blockCollideHandler) {
            this.blockCollideHandler = blockCollideHandler;
            return this;
        }

        protected void triggerHit(Player player) {
            this.hitHandler.accept(player);
        }

        protected void triggerHitEvent(EntityDamageByEntityEvent event) {
            this.eventHandler.accept(event);
        }

        protected void triggerMiss() {
            this.missHandler.run();
        }

        // TODO(port)
//        protected void triggerCollideEntity(ProjectileCollideEvent event) {
//            this.entityCollideHandler.accept(event.getHit());
//        }

        protected void triggerCollideBlock(ProjectileHitEvent event) {
            this.blockCollideHandler.accept(event);
        }
    }
}
