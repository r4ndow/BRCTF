package com.mcpvp.common.util;

import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.task.EasyTask;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldBorder;
import net.minecraft.server.v1_8_R3.WorldBorder;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFirework;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class EffectUtil {

    public static void fakeLightning(Location location) {
        World world = location.getWorld();
        Block block = world.getHighestBlockAt(location.getBlockX(), location.getBlockZ());
        if (block != null) {
            location = location.clone();
            location.setY(Math.min(world.getMaxHeight(), block.getY()));
            world.strikeLightningEffect(location);
        }
    }

    public static BukkitRunnable trail(Entity entity, ParticlePacket particle) {
        return EasyTask.of(task -> {
            if (entity.isDead()) {
                task.cancel();
                return;
            }

            if (entity instanceof Projectile p && p.isOnGround()) {
                task.cancel();
                return;
            }

            particle.at(entity.getLocation()).send();
        });
    }

    public static BukkitRunnable colorTrail(Entity entity, Color color) {
        return trail(entity, ParticlePacket.colored(color));
    }

    public static void sendBorderEffect(Player player) {
        org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer cp = (org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player;

        net.minecraft.server.v1_8_R3.WorldBorder w = new WorldBorder();
        w.setSize(100000);
        w.setCenter(player.getLocation().getX(), player.getLocation().getZ());
        w.setWarningDistance(1000000);
        cp.getHandle().playerConnection.sendPacket(
            new PacketPlayOutWorldBorder(w, PacketPlayOutWorldBorder.EnumWorldBorderAction.INITIALIZE)
        );
    }

    public static void resetBorderEffect(Player player) {
        org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer cp = (org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player;

        WorldBorder ww = new WorldBorder();
        ww.setSize(30_000_000);
        ww.setCenter(player.getLocation().getX(), player.getLocation().getZ());
        cp.getHandle().playerConnection.sendPacket(
            new PacketPlayOutWorldBorder(ww, PacketPlayOutWorldBorder.EnumWorldBorderAction.INITIALIZE)
        );
    }

    public static void sendInstantFirework(FireworkEffect effect, Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.clearEffects();
        meta.addEffect(effect);
        firework.setFireworkMeta(meta);
        ((CraftFirework) firework).getHandle().expectedLifespan = 1;
        WorldServer w = ((CraftWorld) location.getWorld()).getHandle();
        w.broadcastEntityEffect(((CraftEntity) firework).getHandle(), (byte) 17);
    }
}
