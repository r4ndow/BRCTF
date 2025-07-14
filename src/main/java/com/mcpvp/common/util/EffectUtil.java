package com.mcpvp.common.util;

import com.mcpvp.common.ParticlePacket;
import com.mcpvp.common.task.EasyTask;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

public class EffectUtil {

    public static void fakeLightning(Location location) {
        World world = location.getWorld();
        Block block = world.getHighestBlockAt(location.getBlockX(), location.getBlockZ());
        if (block != null) {
            location = location.clone();
            location.setY(Math.min(world.getMaxHeight(), block.getY() ));
            world.strikeLightningEffect(location);
        }
    }

    public static BukkitRunnable colorTrail(Entity entity, Color color) {
        return EasyTask.of(task -> {
            if (entity.isDead()) {
                task.cancel();
                return;
            }

            ParticlePacket
                .colored(color)
                .at(entity.getLocation())
                .send();
        });
    }

}
