package com.mcpvp.battle.game.state;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.PlayerParticipateEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

/**
 * A state handler for outside the game, i.e. before and after but not during.
 */
public class BattleOutsideGameStateHandler extends BattleGameStateHandler {

    private final BattleGameState state;

    public BattleOutsideGameStateHandler(BattlePlugin plugin, BattleGame game, BattleGameState state) {
        super(plugin, game);
        this.state = state;
    }

    @Override
    public void enterState() {
        super.enterState();

        int seconds = switch (state) {
            case BEFORE -> plugin.getBattle().getOptions().getGame().getSecondsBeforeGame();
            case AFTER -> plugin.getBattle().getOptions().getGame().getSecondsAfterGame();
            default -> 15;
        };
        plugin.getBattle().getMatch().getTimer().setSeconds(seconds);

        setupFlags();
    }

    private void setupFlags() {
        game.getTeamManager().getTeams().forEach(bt -> {
            bt.getFlag().reset();
            bt.getFlag().setLocked(true);
        });
    }

    @EventHandler
    public void onParticipate(PlayerParticipateEvent event) {
        event.getPlayer().teleport(game.getConfig().getSpawn());
        event.getPlayer().getInventory().clear();
        event.getPlayer().getInventory().setArmorContents(new ItemStack[4]);
        event.getPlayer().setHealth(event.getPlayer().getMaxHealth());
        event.getPlayer().setExp(0);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // For when players fall into the void
        event.getEntity().teleport(game.getConfig().getSpawn());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // For when players fall into the void
        event.setRespawnLocation(game.getConfig().getSpawn());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo().getY() < 0) {
            event.getPlayer().teleport(game.getConfig().getSpawn());
        }
    }

}
