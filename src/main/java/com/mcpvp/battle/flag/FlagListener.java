package com.mcpvp.battle.flag;

import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagDropEvent;
import com.mcpvp.battle.event.FlagPickupEvent;
import com.mcpvp.battle.event.FlagRecoverEvent;
import com.mcpvp.battle.event.FlagStealEvent;
import com.mcpvp.battle.event.FlagTakeEvent;
import com.mcpvp.battle.event.PlayerResignEvent;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.event.TickEvent;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

@Getter
@AllArgsConstructor
public class FlagListener implements EasyListener {
	
	private final BattlePlugin plugin;
	private final BattleGame game;

	@EventHandler
	public void onTick(TickEvent event) {
		game.getTeamManager().getTeams().forEach(bt -> {
			IBattleFlag flag = bt.getFlag();
			if (flag.isDropped() && flag.getRestoreExpiration().isExpired()) {
				flag.reset();
			}
		});
	}
	
	@EventHandler
	public void onPickup(PlayerPickupItemEvent event) {
		BattleTeam team = game.getTeamManager().getTeam(event.getPlayer());
		game.getTeamManager().getTeams().forEach(bt -> {
			if (!bt.getFlag().isItem(event.getItem().getItemStack())) {
				return;
			}
			
			if (bt != team) {
				if (bt.getFlag().isHome()) {
					if (new FlagStealEvent(event.getPlayer(), bt.getFlag()).call()) {
						event.setCancelled(true);
					}
				} else {
					if (new FlagPickupEvent(event.getPlayer(), bt.getFlag()).call()) {
						event.setCancelled(true);
					}
				}
			} else if (!bt.getFlag().isHome()) {
				new FlagRecoverEvent(event.getPlayer(), bt.getFlag()).call();
				event.setCancelled(true);
			} else {
				event.setCancelled(true);
			}
		});
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onItemDrop(PlayerDropItemEvent event) {
		game.getTeamManager().getTeams().forEach(bt -> {
			if (bt.getFlag().isItem(event.getItemDrop().getItemStack())) {
				new FlagDropEvent(event.getPlayer(), bt.getFlag(), event.getItemDrop()).call();
			}
		});
	}
	
	@EventHandler
	public void onTakeLocked(FlagTakeEvent event) {
		if (event.getFlag().isLocked()) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onItemMerge(ItemMergeEvent event) {
		game.getTeamManager().getTeams().forEach(bt -> {
			if (bt.getFlag().isItem(event.getEntity().getItemStack())) {
				event.setCancelled(true);
			}
		});
	}
	
	@EventHandler
	public void onItemDespawn(ItemDespawnEvent event) {
		game.getTeamManager().getTeams().forEach(bt -> {
			if (bt.getFlag().isItem(event.getEntity().getItemStack())) {
				event.setCancelled(true);
			}
		});
	}

	@EventHandler
	public void onResign(PlayerResignEvent event) {
		game.getTeamManager().getTeams().forEach(bt -> {
			if (bt.getFlag().getCarrier() == event.getPlayer()) {
				new FlagDropEvent(event.getPlayer(), bt.getFlag(), null).call();
			}
		});
	}
	
}
