package com.mcpvp.battle.flag.display;

import com.mcpvp.common.util.nms.ActionbarUtil;
import com.mcpvp.common.util.nms.TitleUtil;
import com.mcpvp.common.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.BiConsumer;

@RequiredArgsConstructor
public enum FlagDisplayChannel {
    CHAT(CommandSender::sendMessage),
    ACTIONBAR(ActionbarUtil::send),
    SUBTITLE((player, message) ->
        TitleUtil.sendTitle(player, "", message, Duration.ZERO, Duration.seconds(2), Duration.seconds(1))
    );

    @Getter
    private final BiConsumer<Player, String> sender;

}
