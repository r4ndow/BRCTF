package com.mcpvp.battle.options;

import com.mcpvp.battle.BattlePlugin;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;

@Getter
@Setter
@Log4j2
public class BattleOptions {
	
	/**
	 * Most options can be read straight from the input file, so we use delegate to make those available.
	 */
	@Delegate
	private final BattleOptionsInput input;
	private final BattlePlugin plugin;
	
	// Below are options parsed on the fly, which rely on the plugin
	
//	private BKitWrapper defaultKitWrapper;
	
	/**
	 * @param plugin The plugin which is creating the options.
	 * @param input The input, read directly from the file.
	 */
	public BattleOptions(BattlePlugin plugin, BattleOptionsInput input) {
		this.plugin = plugin;
		this.input = input;
	}
	
//	public BKitWrapper getDefaultKitWrapper() {
//		defaultKitWrapper = plugin.getKits().find(input.getDefaultKit());
//		if (defaultKitWrapper == null) {
//			log.warn("The default kit wrapper of " + input.getDefaultKit() + " could not be found");
//			defaultKitWrapper = plugin.getKits().find(HeavyKit.class);
//		}
//		return defaultKitWrapper;
//	}
	
}
