package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.config.BattleCallout;
import com.mcpvp.common.util.chat.C;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class QuickCommand extends BattleCommand {

    private static final double RADIUS = 15;
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
        String callout = findClosestCallout(asPlayer(sender).getLocation())
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

    private Optional<BattleCallout> findClosestCallout(Location location) {
        return battle.getGame().getConfig().getCallouts().stream()
                .filter(callout -> callout.getLocation().distance(location) <= RADIUS)
                .min(Comparator.comparingDouble(c -> c.getLocation().distanceSquared(location)));
    }

}
