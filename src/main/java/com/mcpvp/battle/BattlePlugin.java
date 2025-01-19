package com.mcpvp.battle;

import com.mcpvp.battle.command.*;
import com.mcpvp.common.event.TickEvent;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
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

        registerCommands();
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
        new StartCommand(battle).register();
        new NextCommand(battle).register();
        new PlaceCommand(battle).register();
        new FlagCommands(battle).register();
        new KitCommand(battle.getKitManager()).register();
        new TimerCommand(battle).register();
        new SwitchCommand(battle).register();
    }

}
