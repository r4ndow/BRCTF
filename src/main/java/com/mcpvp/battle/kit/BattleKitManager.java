package com.mcpvp.battle.kit;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kits.global.NecroRevivalTagManager;
import com.mcpvp.battle.kits.global.ScoutDeathTagManager;
import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.kit.KitManager;
import com.mcpvp.common.chat.C;
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
    private final ScoutDeathTagManager scoutDeathTagManager;
    @Getter
    private final NecroRevivalTagManager necroRevivalTagManager;

    public BattleKitManager(Plugin plugin, Battle battle) {
        super(plugin);
        this.battle = battle;
        this.scoutDeathTagManager = new ScoutDeathTagManager((BattlePlugin) plugin);
        this.necroRevivalTagManager = new NecroRevivalTagManager((BattlePlugin) plugin);
    }

    @Override
    public List<KitDefinition> getKitDefinitions() {
        return Arrays.asList(BattleKitType.values());
    }

    public void setDisabled(KitDefinition kit) {
        this.disabled.add(kit);

        // Any players who are playing this Kit should have it force changed
        this.battle.getGame().getParticipants().forEach(player -> {
            if (this.battle.getKitManager().isSelected(player, kit)) {
                player.sendMessage(C.info(C.GOLD) + C.hl(kit.getName()) + " has been disabled");
                // Calling `setSelected` will auto re-spawn/re-equip them
                this.battle.getKitManager().setSelected(player, BattleKitType.HEAVY, true);
            }
        });
    }

    public void setEnabled(KitDefinition kit) {
        this.disabled.remove(kit);
    }

    public boolean isDisabled(KitDefinition kit) {
        return this.disabled.contains(kit);
    }

    public void setLimit(KitDefinition kit, int limit) {
        this.limited.put(kit, limit);

        // Any players who are playing this Kit should have it force changed
        this.battle.getGame().getTeamManager().getTeams().forEach(team -> {
            int count = 0;
            for (Player player : team.getPlayers()) {
                if (this.battle.getKitManager().isSelected(player, kit)) {
                    if (count++ == limit) {
                        player.sendMessage(C.info(C.GOLD) + C.hl(kit.getName()) + " has been limited");
                        // Calling `setSelected` will auto re-spawn/re-equip them
                        this.battle.getKitManager().setSelected(player, BattleKitType.HEAVY, true);
                    }
                }
            }
        });
    }

    public void removeLimit(KitDefinition kit) {
        this.limited.remove(kit);
    }

    public Optional<Integer> getLimit(KitDefinition kit) {
        return Optional.ofNullable(this.limited.getOrDefault(kit, null));
    }

    @Nullable
    @Override
    public BattleKit get(Player player) {
        return (BattleKit) super.get(player);
    }
}
