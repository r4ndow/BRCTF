package com.mcpvp.common.util.nms;

import com.mcpvp.common.time.Duration;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class TitleUtil {

    public static void sendTitle(Player player, String title, String subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
        resetTitle(player);

        // Send timings first, if set
        if (fadeIn != null && stay != null && fadeOut != null) {
            PacketUtil.sendPacket(player, new PacketPlayOutTitle(fadeIn.toTicks(), stay.toTicks(), fadeOut.toTicks()));
        }

        // Send title
        PacketUtil.sendPacket(player, new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, new ChatComponentText(ChatColor.translateAlternateColorCodes('&', title))));

        // Send subtitle if present
        if (!subtitle.isEmpty()) {
            PacketUtil.sendPacket(player, new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, new ChatComponentText(ChatColor.translateAlternateColorCodes('&', subtitle))));
        }
    }

    public static void clearTitle(Player player) {
        PacketUtil.sendPacket(player, new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.CLEAR, null));
    }

    public static void resetTitle(Player player) {
        PacketUtil.sendPacket(player, new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.RESET, null));
    }

}
