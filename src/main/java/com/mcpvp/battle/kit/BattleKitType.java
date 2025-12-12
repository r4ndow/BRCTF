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
    CHEMIST("Chemist", ChemistKit::new),
    DWARF("Dwarf", DwarfKit::new),
    ELF("Elf", ElfKit::new),
    ENGINEER("Engineer", EngineerKit::new),
    HEAVY("Heavy", HeavyKit::new),
    MAGE("Mage", MageKit::new),
    MEDIC("Medic", MedicKit::new),
    NECRO("Necro", NecroKit::new),
    NINJA("Ninja", NinjaKit::new),
    PYRO("Pyro", PyroKit::new),
    SCOUT("Scout", ScoutKit::new),
    SOLDIER("Soldier", SoldierKit::new),
    VAMPIRE("Vampire", VampireKit::new),
    THIEF("Thief", ThiefKit::new),
    MAGE2("Mage2", Mage2Kit::new),
    NINJA2("Ninja2", Ninja2Kit::new),
    MAGE3("Mage3", Mage3Kit::new),
    BATTLEMAGE("Battlemage", BattlemageKit::new),
    TRICKSTER("Trickster", TricksterKit::new),
    ;

    @Getter
    private final String name;
    private final BiFunction<BattlePlugin, Player, Kit> creator;

    @Override
    public Kit create(Plugin plugin, Player player) {
        return this.creator.apply((BattlePlugin) plugin, player);
    }

}
