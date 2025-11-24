package com.mcpvp.battle.team;

import com.mcpvp.battle.config.BattleTeamConfig;
import com.mcpvp.battle.flag.BattleFlag;
import com.mcpvp.battle.flag.FlagManager;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.chat.Colors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
@ToString(of = {"id", "name"})
public class BattleTeam {

    private final int id;
    private final Set<Player> players = new HashSet<>();
    private final BattleTeamConfig config;
    private String name;
    private Colors color;
    @Setter
    private int captures;
    @Setter
    private BattleFlag flag;
    @Setter
    private FlagManager flagManager;

    public BattleTeam(int id, String name, Colors color, BattleTeamConfig config) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.config = config;
    }

    void add(Player player) {
        this.players.add(player);
    }

    void remove(Player player) {
        this.players.remove(player);
    }

    public boolean contains(Player player) {
        return this.players.contains(player);
    }

    public void onCapture() {
        this.captures++;
    }

    public boolean isInSpawn(Player player) {
        return this.isInSpawn(player.getLocation());
    }

    public boolean isInSpawn(Location location) {
        Block underFeet = location.getBlock().getRelative(BlockFace.DOWN);
        Block spawnBlock = this.config.getSpawn().getBlock().getRelative(BlockFace.DOWN);
        return underFeet.getType() == spawnBlock.getType();
    }

    public String getColoredName() {
        return this.getColor() + this.getName() + C.R;
    }

}
