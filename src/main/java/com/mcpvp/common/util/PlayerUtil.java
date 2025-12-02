package com.mcpvp.common.util;

import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

public class PlayerUtil {

    /**
     * Reset as many of the player's statuses or properties as possible, eg health, fire ticks, velocity, etc.
     *
     * @param player The player to reset.
     */
    public static void reset(Player player) {
        PlayerUtil.setAbsorptionHearts(player, 0);
        player.setHealth(player.getMaxHealth());
        player.setFireTicks(0);
        player.setLevel(0);
        player.setExp(1.0f);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getActivePotionEffects().stream().map(PotionEffect::getType).forEach(player::removePotionEffect);
        player.setHealth(player.getMaxHealth());
        player.setVelocity(new Vector());
    }

    public static float getAbsorptionHearts(Player player) {
        return asCraftPlayer(player).getAbsorptionHearts();
    }

    public static void setAbsorptionHearts(Player player, float hearts) {
        asCraftPlayer(player).setAbsorptionHearts(hearts);
    }

    public static EntityPlayer asCraftPlayer(Player player) {
        return ((org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer) player).getHandle();
    }

}
