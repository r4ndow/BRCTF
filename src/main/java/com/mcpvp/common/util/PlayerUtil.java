package com.mcpvp.common.util;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.entity.Player;

public class PlayerUtil {

    public static float getAbsorptionHearts(Player player) {
        return asCraftPlayer(player).getAbsorptionHearts();
    }

    public static void setAbsorptionHearts(Player player, float hearts) {
        asCraftPlayer(player).setAbsorptionHearts(hearts);
    }

    private static EntityPlayer asCraftPlayer(Player player) {
        return ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle();
    }

}
