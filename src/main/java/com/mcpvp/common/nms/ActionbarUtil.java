package com.mcpvp.common.nms;

import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.entity.Player;

public class ActionbarUtil {

    public static void send(Player player, String message) {
        PacketUtil.sendPacket(player, createActionBarPacket(message));
    }

    private static PacketPlayOutChat createActionBarPacket(String message) {
        String msg = "{\"text\":\"" + message + "\"}";
        return new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a(msg), (byte) 2);
    }

}
