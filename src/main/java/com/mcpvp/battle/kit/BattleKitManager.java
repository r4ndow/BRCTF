package com.mcpvp.battle.kit;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kits.global.ScoutDeathTagManager;
import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.kit.KitManager;
import com.mcpvp.common.util.chat.C;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.*;

public class BattleKitManager extends KitManager {

    private final Battle battle;
    private final List<KitDefinition> disabled = new ArrayList<>();
    private final Map<KitDefinition, Integer> limited = new HashMap<>();

    @Getter
    private final ScoutDeathTagManager globalScoutKit;

    public BattleKitManager(Plugin plugin, Battle battle) {
        super(plugin);
        this.battle = battle;
        this.globalScoutKit = new ScoutDeathTagManager((BattlePlugin) plugin);
    }

    @Override
    public List<KitDefinition> getKitDefinitions() {
        return Arrays.asList(BattleKitType.values());
    }

    public void setDisabled(KitDefinition kit) {
        disabled.add(kit);

        // Any players who are playing this Kit should have it force changed
        battle.getGame().getParticipants().forEach(player -> {
            if (battle.getKitManager().isSelected(player, kit)) {
                player.sendMessage(C.info(C.GOLD) + C.hl(kit.getName()) + " has been disabled");
                // Calling `setSelected` will auto re-spawn/re-equip them
                battle.getKitManager().setSelected(player, BattleKitType.HEAVY, true);
            }
        });
    }

    public void setEnabled(KitDefinition kit) {
        disabled.remove(kit);
    }

    public boolean isDisabled(KitDefinition kit) {
        return disabled.contains(kit);
    }

    public void setLimit(KitDefinition kit, int limit) {
        limited.put(kit, limit);

        // Any players who are playing this Kit should have it force changed
        battle.getGame().getTeamManager().getTeams().forEach(team -> {
            int count = 0;
            for (Player player : team.getPlayers()) {
                if (battle.getKitManager().isSelected(player, kit)) {
                    if (count++ == limit) {
                        player.sendMessage(C.info(C.GOLD) + C.hl(kit.getName()) + " has been limited");
                        // Calling `setSelected` will auto re-spawn/re-equip them
                        battle.getKitManager().setSelected(player, BattleKitType.HEAVY, true);
                    }
                }
            }
        });
    }

    public void removeLimit(KitDefinition kit) {
        limited.remove(kit);
    }

    public Optional<Integer> getLimit(KitDefinition kit) {
        return Optional.ofNullable(limited.getOrDefault(kit, null));
    }

    @Nullable
    @Override
    public BattleKit get(Player player) {
        return (BattleKit) super.get(player);
    }
}
