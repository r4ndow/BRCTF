package com.mcpvp.battle.map.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.map.repo.BattleMapSource;
import com.mcpvp.battle.options.BattleOptionsInput;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class MergingMapManager implements BattleMapManager {

    private final BattlePlugin plugin;
    private final BattleOptionsInput.MapOptions mapOptions;
    private final List<BattleMapSource> repos;

    @Override
    public List<BattleMapData> getEnabled() {
        return this.repos.stream().flatMap(repo -> repo.getFunctional().stream()).filter(m ->
            this.mapOptions.getCategories().getOrDefault(m.getCategory(), true)
        ).filter(m ->
            !this.mapOptions.getDisable().contains(m.getId())
        ).toList();
    }

    @Override
    public boolean isMap(int id) {
        return this.repos.stream().anyMatch(repo ->
            repo.getFunctional().stream().anyMatch(d -> d.getId() == id)
        );
    }

    @Override
    public List<Integer> pickMaps(int games) {
        List<BattleMapData> eligible = new ArrayList<>();
        this.repos.forEach(repo -> eligible.addAll(this.getEnabled()));
        Collections.shuffle(eligible);

        List<Integer> maps = new ArrayList<>();

        for (int i = 0; i < games; i++) {
            maps.add(eligible.get(i % eligible.size()).getId());
        }

        return maps;
    }

    @Override
    public BattleMapData loadMap(int id) {
        return this.repos.stream()
            .flatMap(repo -> repo.getFunctional().stream())
            .filter(m -> m.getId() == id)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Could not find the requested map: " + id));
    }

    @Override
    public List<BattleMapData> loadMaps(int games) {
        return this.pickMaps(games).stream().map(this::loadMap).toList();
    }

    @Override
    public File getWorldData(BattleMapData map) {
        return this.repos.stream()
            .filter(repo -> repo.getAll().contains(map))
            .findFirst()
            .map(repo -> repo.getWorldData(map))
            .orElseThrow(() -> new IllegalStateException("No world data found for map: " + map));
    }

    @Override
    public void setOverride(List<Integer> ids) {
        File file = new File(this.plugin.getDataFolder(), "override_maps.json");
        try {
            FileUtils.write(file, new ObjectMapper().writeValueAsString(ids));
        } catch (IOException e) {
            throw new RuntimeException("Unable to create map override", e);
        }
    }

    @Override
    public void clearOverride() {
        if (!new File(this.plugin.getDataFolder(), "override_maps.json").delete()) {
            throw new RuntimeException("Failed to remove the override_maps.json");
        }
    }

    @Override
    public List<BattleMapData> getOverride() {
        File file = new File(this.plugin.getDataFolder(), "override_maps.json");
        if (!file.exists()) {
            return Collections.emptyList();
        }
        try {
            List<Integer> ids = new ObjectMapper().readValue(file, new TypeReference<>() {
            });
            return ids.stream().map(this::loadMap).toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the override_maps.json file", e);
        }
    }

}
