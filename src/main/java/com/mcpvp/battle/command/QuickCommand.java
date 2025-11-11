package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.command.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuickCommand extends Command {

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
    public boolean execute(CommandSender sender, String label, String[] args) {
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

    protected Player asPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new IllegalStateException("Only players can execute this command.");
        }

        return ((Player) sender);
    }

    public QuickCommand all() {
        this.sendToAll = true;
        return this;
    }

    public QuickCommand loc() {
        this.includeLocation = true;
        return this;
    }

    public void register() {
        CommandUtil.getCommandMap().register("mcctf", this);
    }

}
