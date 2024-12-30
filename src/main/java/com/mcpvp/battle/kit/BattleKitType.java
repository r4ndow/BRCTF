package com.mcpvp.battle.kit;

import java.util.function.BiFunction;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kits.ArcherKit;
import com.mcpvp.battle.kits.HeavyKit;
import com.mcpvp.battle.kits.SoldierKit;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitDefinition;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BattleKitType implements KitDefinition {
    ARCHER("Archer", ArcherKit::new),
    SOLDIER("Soldier", SoldierKit::new),
    HEAVY("Heavy", HeavyKit::new),
    ;

    @Getter
    private final String name;
    private final BiFunction<BattlePlugin, Player, Kit> creator;

    @Override
    public Kit create(Plugin plugin, Player player) {
        return creator.apply((BattlePlugin) plugin, player);
    }
    
}
