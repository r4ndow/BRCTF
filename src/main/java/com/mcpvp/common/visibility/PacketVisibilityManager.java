//package com.mcpvp.common.visibility;
//
//import com.comphenix.protocol.PacketType;
//import com.comphenix.protocol.ProtocolManager;
//import com.comphenix.protocol.events.ListenerPriority;
//import com.comphenix.protocol.events.PacketAdapter;
//import com.comphenix.protocol.events.PacketEvent;
//import com.comphenix.protocol.wrappers.EnumWrappers;
//import com.comphenix.protocol.wrappers.PlayerInfoData;
//import com.mcpvp.common.event.EasyListener;
//import lombok.Getter;
//import lombok.extern.log4j.Log4j2;
//import org.bukkit.Bukkit;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.Plugin;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Log4j2
//public class PacketVisibilityManager implements VisibilityManager, EasyListener {
//
//    @Getter
//    private final Plugin plugin;
//    // {Observer -> [Hidden Targets]}
//    private final Map<Player, List<Player>> hiddenTo = new HashMap<>();
//    private final ProtocolManager protocolManager;
//
//    public PacketVisibilityManager(Plugin plugin, ProtocolManager protocolManager) {
//        this.plugin = plugin;
//        this.protocolManager = protocolManager;
//    }
//
//    @Override
//    public void init() {
//        protocolManager.addPacketListener(new PlayerPacketInterceptor());
//    }
//
//    @Override
//    public void hide(Player observer, Player target) {
//        hiddenTo.computeIfAbsent(observer, k -> new ArrayList<>()).add(target);
//        observer.hidePlayer(target);
//    }
//
//    @Override
//    public void show(Player observer, Player target) {
//        if (hiddenTo.containsKey(observer)) {
//            hiddenTo.get(observer).remove(target);
//        }
//        observer.showPlayer(target);
//    }
//
//    @Override
//    public boolean canSee(Player observer, Player target) {
//        return !(hiddenTo.containsKey(observer) && hiddenTo.get(observer).contains(target));
//    }
//
//    private class PlayerPacketInterceptor extends PacketAdapter {
//
//        public PlayerPacketInterceptor() {
//            super(PacketVisibilityManager.this.plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO);
//        }
//
//        @Override
//        public void onPacketSending(PacketEvent event) {
//            // Allow player removal packets to go through
//            if (event.getPacket().getPlayerInfoAction().read(0) == EnumWrappers.PlayerInfoAction.REMOVE_PLAYER) {
//                return;
//            }
//
//            log.info("Handle packet {}", event);
//
//            Player observer = getPlayer(event.getPlayer().getEntityId());
//            List<PlayerInfoData> targets = new ArrayList<>();
//
//            // Find all target players who should be able to receive this packet
//            for (PlayerInfoData playerInfoData : event.getPacket().getPlayerInfoDataLists().read(0)) {
//                Player target = Bukkit.getPlayer(playerInfoData.getProfile().getUUID());
//                if (target == null) {
//                    continue;
//                }
//
//                log.info("Test if {} can see {}, ={}", observer.getName(), target.getName(), canSee(observer, target));
//                if (canSee(observer, target)) {
//                    targets.add(playerInfoData);
//                }
//            }
//
//            if (targets.isEmpty()) {
//                event.setCancelled(true);
//            } else {
//                event.getPacket().getPlayerInfoDataLists().write(0, targets);
//            }
//        }
//
//        private Player getPlayer(int entityId) {
//            return Bukkit.getOnlinePlayers().stream()
//                .filter(p -> p.getEntityId() == entityId)
//                .findAny()
//                .orElse(null);
//        }
//
//    }
//
//}
