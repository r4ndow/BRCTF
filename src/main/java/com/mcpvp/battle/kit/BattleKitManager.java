package com.mcpvp.battle.kit;

import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.kit.KitManager;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class BattleKitManager extends KitManager {

    public BattleKitManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public List<KitDefinition> getKitDefinitions() {
        return Arrays.asList(BattleKitType.values());
    }

}
