package com.mcpvp.battle.map.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.battle.map.BattleMapData;
import com.mcpvp.battle.options.BattleOptionsInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Log4j2
@RequiredArgsConstructor
public class LocalMapRepo implements MapRepo {

    private final ObjectMapper mapper;
    private final BattleOptionsInput.CentralMapSourceOptions mapOptions;
    private final List<BattleMapData> mapData = new ArrayList<>();

    @Override
    public void init() {
        File mapsDir = new File(mapOptions.getDir());
        if (!mapsDir.exists()) {
            throw new IllegalStateException("Maps directory does not exist: " + mapsDir);
        }

        String mapsJson = loadMapsJson();

        try {
            this.mapData.addAll(mapper.readValue(
                mapsJson, new TypeReference<List<BattleMapData>>() {
                }
            ));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read maps.json", e);
        }

        check();
    }

    private String loadMapsJson() {
        String json = null;

        // Try to read the configuration supplied file first
        File configured = new File(mapOptions.getJson());
        if (configured.exists()) {
            try {
                return Objects.requireNonNull(FileUtils.readFileToString(configured));
            } catch (IOException e) {
                log.warn("Failed to read custom maps.json at {}", mapOptions.getJson(), e);
            }
        }

        // Otherwise, use the included maps.json
        try (InputStream included = this.getClass().getResourceAsStream("/maps.json")) {
            try {
                json = IOUtils.toString(Objects.requireNonNull(included), Charset.defaultCharset());
            } catch (IOException e) {
                log.warn("Failed to read included maps.json", e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (json == null) {
            throw new RuntimeException("Could not load maps.json");
        }

        return json;
    }

    /**
     * Verify the found map data.
     */
    private void check() {
        if (this.mapData.isEmpty()) {
            throw new IllegalStateException("No map data found");
        }

        this.mapData.forEach(data -> {
            File mapData = new File(mapOptions.getDir(), data.getFile());

            if (!mapData.exists()) {
                log.warn("Map file was not found. Expected at: {}", mapData);
                data.setFunctional(false);
            }
        });
    }

    @Override
    public List<BattleMapData> getAll() {
        return mapData;
    }

    @Override
    public File getWorldData(BattleMapData map) {
        return new File(mapOptions.getDir(), map.getFile());
    }

}
