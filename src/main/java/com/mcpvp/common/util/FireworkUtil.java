package com.mcpvp.common.util;

import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFirework;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

public class FireworkUtil {

    public static void explodeInstantly(FireworkEffect effect, Location location) {
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
