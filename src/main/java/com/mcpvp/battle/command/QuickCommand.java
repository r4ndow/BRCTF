package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.common.chat.C;
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
        String callout = this.battle.getGame().findClosestCallout(this.asPlayer(sender).getLocation())
            .map(c -> {
                if (c.getConfig() != null) {
                    return " (near %s %s)".formatted(
                        this.battle.getGame().getTeamManager().getTeam(c.getConfig().getId()).getName(), c.getText()
                    );
                }
                return " (near %s)".formatted(c.getText());
            })
            .orElse("");

        this.asPlayer(sender).chat("%s%s/%s%s %s%s".formatted(
            this.sendToAll ? "!" : "",
            C.PURPLE,
            this.getName(),
            C.R,
            this.message,
            this.includeLocation ? callout : ""
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
