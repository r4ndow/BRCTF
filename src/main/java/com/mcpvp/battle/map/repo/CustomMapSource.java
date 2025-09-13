package com.mcpvp.battle.map.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.options.BattleOptionsInput;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Support for a custom map repository, which is a folder that contains maps.
 * The configuration of the maps is decentralized: each world folder also contains
 * a "ctf.json" file for configuration.
 */
@RequiredArgsConstructor
public class CustomMapSource implements BattleMapSource {

    private final ObjectMapper mapper;
    private final BattleOptionsInput.CustomMapSourceOptions options;

    @Override
    public void init() {
        if (!new File(options.getDir()).exists()) {
            throw new IllegalArgumentException("Expected a custom map repository, but it does not exist at path %s".formatted(
                new File(options.getDir()).getAbsolutePath()
            ));
        }
    }

    @Override
    public List<BattleMapData> getAll() {
        List<BattleMapData> maps = new ArrayList<>();
        for (File map : Objects.requireNonNull(new File(options.getDir()).listFiles())) {
            File data = new File(map, "ctf.json");
            if (!data.exists()) {
                continue;
            }

            try {
                maps.add(mapper.readValue(data, BattleMapData.class));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read map data for folder %s".formatted(map.getAbsolutePath()), e);
            }
        }

        return maps;
    }

    @Override
    public File getWorldData(BattleMapData map) {
        return new File(options.getDir(), map.getFile());
    }

}
