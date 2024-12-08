package com.mcpvp.battle.map.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.map.repo.MapRepo;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class LocalMapManager implements MapManager {
	
	private final BattlePlugin plugin;
	private final MapRepo repo;
	
	@Override
	public void init() {
	}
	
	@Override
	public boolean isMap(int id) {
		return repo.getFunctional().stream().anyMatch(d -> d.getId() == id);
	}
	
	@Override
	public List<Integer> pickMaps(int games) {
		List<BattleMapData> eligible = new ArrayList<>(repo.getEnabled());
		Collections.shuffle(eligible);

		List<Integer> maps = new ArrayList<>();

		for (int i = 0; i < games; i++)
			maps.add(eligible.get(i % eligible.size()).getId());
		
		return maps;
	}
	
	@Override
	public BattleMapData loadMap(int id) {
		return repo.getAll().stream().filter(m -> m.getId() == id).findFirst().orElse(null);
	}
	
	@Override
	public List<BattleMapData> loadMaps(int games) {
		return pickMaps(games).stream().map(this::loadMap).toList();
	}
	
	@Override
	public void setOverride(List<Integer> ids) {
		File file = new File(plugin.getDataFolder(), "override_maps.json");
		try {
			FileUtils.write(file, new ObjectMapper().writeValueAsString(ids));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public List<BattleMapData> getOverride() {
		File file = new File(plugin.getDataFolder(), "override_maps.json");
		if (!file.exists()) {
			return Collections.emptyList();
		}
		try {
			List<Integer> ids = new ObjectMapper().readValue(file, new TypeReference<List<Integer>>() {
			});
			return ids.stream().map(this::loadMap).toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
