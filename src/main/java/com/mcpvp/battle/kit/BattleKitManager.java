package com.mcpvp.battle.kit;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.kits.global.AssassinCooldownManager;
import com.mcpvp.battle.kits.global.NecroRevivalTagManager;
import com.mcpvp.battle.kits.global.ScoutDeathTagManager;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.kit.KitDefinition;
import com.mcpvp.common.kit.KitManager;
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
    @Getter
    private final AssassinCooldownManager assassinCooldownManager;

    public BattleKitManager(Plugin plugin, Battle battle) {
        super(plugin);
        this.battle = battle;
        this.scoutDeathTagManager = new ScoutDeathTagManager((BattlePlugin) plugin);
        this.necroRevivalTagManager = new NecroRevivalTagManager((BattlePlugin) plugin);
        this.assassinCooldownManager = new AssassinCooldownManager();
    }

    @Override
    public List<KitDefinition> getKitDefinitions() {
        return Arrays.asList(BattleKitType.values());
    }

    @Override
    public KitDefinition getKitDefinition(String name) {
        // First, try the normal lookup (English names, e.g. "archer", "heavy").
        KitDefinition definition = super.getKitDefinition(name);
        if (definition != null) {
            return definition;
        }

        if (name == null) {
            return null;
        }

        String normalized = name.toLowerCase(java.util.Locale.ROOT);

        switch (normalized) {
            case "arqueiro":
                return BattleKitType.ARCHER;

            case "assassino":
                return BattleKitType.ASSASSIN;

            case "quimico":
            case "químico":
                return BattleKitType.CHEMIST;

            case "anao":
            case "anão":
                return BattleKitType.DWARF;

            case "elfo":
                return BattleKitType.ELF;

            case "engenheiro":
                return BattleKitType.ENGINEER;

            case "guerreiro":
                return BattleKitType.HEAVY;

            case "mago":
                return BattleKitType.MAGE;

            case "medico":
            case "médico":
                return BattleKitType.MEDIC;

            case "incendiario":
            case "incendiário":
                return BattleKitType.PYRO;

            case "batedor":
                return BattleKitType.SCOUT;

            case "soldado":
                return BattleKitType.SOLDIER;

            // new aliases:
            case "ladrao":
            case "ladrão":
                return BattleKitType.THIEF;

            case "vampiro":
                return BattleKitType.VAMPIRE;

            case "mago2":
                return BattleKitType.MAGE2;

            default:
                return null;
        }
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

    public Optional<BattleKit> find(Player player) {
        return Optional.ofNullable((BattleKit) super.get(player));
    }

}
