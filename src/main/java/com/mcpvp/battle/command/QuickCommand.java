package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.common.util.chat.C;
import org.bukkit.command.CommandSender;

import java.util.List;

public class QuickCommand extends BattleCommand {

    private final Battle battle;
    private final String message;
    private boolean sendToAll;
    private boolean includeLocation;

    protected QuickCommand(Battle battle, String name, String message) {
        super(name);
        this.battle = battle;
        this.message = message;
    }

    @Override
    public boolean onCommand(CommandSender sender, String label, List<String> args) {
        String callout = battle.getGame().findClosestCallout(asPlayer(sender).getLocation())
                .map(c -> {
                    if (c.getConfig() != null) {
                        return " (near %s %s)".formatted(
                                battle.getGame().getTeamManager().getTeam(c.getConfig().getId()).getName(), c.getText()
                        );
                    }
                    return " (near %s)".formatted(c.getText());
                })
                .orElse("");

        asPlayer(sender).chat("%s%s/%s%s %s%s".formatted(
                sendToAll ? "!" : "",
                C.PURPLE,
                this.getName(),
                C.R,
                message,
                includeLocation ? callout : ""
        ));
        return false;
    }

    public QuickCommand all() {
        this.sendToAll = true;
        return this;
    }

    public QuickCommand loc() {
        this.includeLocation = true;
        return this;
    }
}
