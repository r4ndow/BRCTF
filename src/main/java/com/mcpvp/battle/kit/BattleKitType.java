package com.mcpvp.battle.kit;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kits.*;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitDefinition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.function.BiFunction;

@RequiredArgsConstructor
public enum BattleKitType implements KitDefinition {
    ARCHER("Archer", ArcherKit::new),
    ASSASSIN("Assassin", AssassinKit::new),
    HEAVY("Heavy", HeavyKit::new),
    MEDIC("Medic", MedicKit::new),
    NINJA("Ninja", NinjaKit::new),
    PYRO("Pyro", PyroKit::new),
    SOLDIER("Soldier", SoldierKit::new),
    ;

    @Getter
    private final String name;
    private final BiFunction<BattlePlugin, Player, Kit> creator;

    @Override
    public Kit create(Plugin plugin, Player player) {
        return creator.apply((BattlePlugin) plugin, player);
    }

}
