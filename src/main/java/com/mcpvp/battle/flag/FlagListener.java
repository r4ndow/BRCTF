package com.mcpvp.battle.flag;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.event.FlagDropEvent;
import com.mcpvp.battle.event.FlagPickupEvent;
import com.mcpvp.battle.event.FlagRecoverEvent;
import com.mcpvp.battle.event.FlagStealEvent;
import com.mcpvp.battle.event.FlagTakeEvent;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.common.event.EasyListener;

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
	private final Battle battle;
	
	@EventHandler
	public void onPickup(PlayerPickupItemEvent event) {
		BattleTeam team = battle.getTeamManager().getTeam(event.getPlayer());
		battle.getTeamManager().getTeams().forEach(bt -> {
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
			} else {
				event.setCancelled(true);
			}
		});
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onItemDrop(PlayerDropItemEvent event) {
		battle.getTeamManager().getTeams().forEach(bt -> {
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
		battle.getTeamManager().getTeams().forEach(bt -> {
			if (bt.getFlag().isItem(event.getEntity().getItemStack())) {
				event.setCancelled(true);
			}
		});
	}
	
	@EventHandler
	public void onItemDespawn(ItemDespawnEvent event) {
		battle.getTeamManager().getTeams().forEach(bt -> {
			if (bt.getFlag().isItem(event.getEntity().getItemStack())) {
				event.setCancelled(true);
			}
		});
	}
	
}
