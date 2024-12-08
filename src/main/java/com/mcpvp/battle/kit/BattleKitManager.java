package com.mcpvp.battle.kit;

import java.util.List;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kits.HeavyKit;
import com.mcpvp.common.kit.KitManager;
import com.mcpvp.common.kit.KitType;

public class BattleKitManager extends KitManager {

    private final List<KitType<?>> KIT_TYPES;

    public BattleKitManager(BattlePlugin plugin) {
        super(plugin);

        KIT_TYPES = List.of(
                new KitType<>(HeavyKit.class, plugin)
        );
    }

    @Override
    public List<KitType<?>> getKitTypes() {
        return KIT_TYPES;
    }

}
