package com.mcpvp.battle;

import com.mcpvp.battle.command.*;
import com.mcpvp.common.event.TickEvent;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class BattlePlugin extends JavaPlugin {

    @Getter
    private Battle battle;

    @SneakyThrows
    @Override
    public void onLoad() {
        super.onLoad();

        this.battle = new Battle(this);
        this.battle.load();

        this.registerCommands();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        final AtomicInteger tick = new AtomicInteger();
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {
                new TickEvent(tick.getAndIncrement()).call();
            } catch (Exception e) {
                log.error("TickEvent error", e);
            }
        }, 1, 1);

        this.battle.start();
    }

    private void registerCommands() {
        new FlagCommands(this.battle).register();
        new KitCommand(this.battle.getKitManager()).register();
        new KitManagerCommands(this.battle.getKitManager()).register();
        new MapCommands(this.battle).register();
        new SwitchCommand(this.battle).register();
        new TimerCommand(this.battle).register();
        new YellCommand().register();

        QuickCommands.registerAll(this.battle);

        this.getCommand("mcctf").setExecutor(new McctfCommand());
    }

    public static class McctfCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
            commandSender.sendMessage("nice");
            return false;
        }

    }

}
