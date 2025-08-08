package com.mcpvp.battle.map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.battle.config.BattleGameConfig;
import com.mcpvp.battle.config.BattleTeamConfig;
import com.mcpvp.battle.map.parser.BattleMapLoader;
import com.mcpvp.battle.map.parser.BattleMapLoaderMetadataImpl;
import com.mcpvp.battle.map.parser.BattleMapLoaderSignImpl;
import com.mcpvp.battle.map.repo.MapRepo;
import com.mcpvp.battle.options.BattleOptionsInput;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@RequiredArgsConstructor
public class BattleMapTester {

    private final ObjectMapper mapper;

    @SneakyThrows
    public void run(
        BattleOptionsInput.MapOptions mapOptions,
        BattleOptionsInput.MapTesterOptions testOptions,
        MapRepo mapRepo
    ) {
        File testOutputDir = new File(testOptions.getOutputDir());
        File testOutputFile = new File(testOutputDir, "test_progress_" + testOptions.getRunId() + ".json");

        MapTestResults results = loadExistingResults(testOutputFile);

        List<BattleMapData> untested = mapRepo.getAll().stream()
            .filter(data -> !results.errors.containsKey(data.getId()))
            .toList();
        log.info("Found {} untested maps. Testing will begin in ten seconds...", untested.size());

        Thread.sleep(10_000);

        for (int i = 0; i < untested.size(); i++) {
            if ((i + 1) % 500 == 0) {
                saveResults(testOutputFile, results);
                log.info("Stopping tester to avoid memory problems. Restart the server to continue testing.");
                Bukkit.shutdown();
                return;
            }

            BattleMapData battleMapData = untested.get(i);
            try {
                log.info("Processing map {}", battleMapData);
                List<String> errors = test(
                    new File(mapOptions.getDir()),
                    battleMapData,
                    i
                );
                results.errors.put(battleMapData.getId(), errors);
            } catch (Exception e) {
                throw new RuntimeException("Error while testing map: " + battleMapData, e);
            }
        }

        log.info("Map testing complete!");
        saveResults(testOutputFile, results);
        generateMapsJson(mapRepo, results, testOptions);

        Bukkit.shutdown();
    }

    private List<String> test(File mapsDirectory, BattleMapData mapData, int index) throws IOException {
        BattleMapLoader parser;
        if (mapData.getMetadata() != null) {
            parser = new BattleMapLoaderMetadataImpl();
        } else {
            parser = new BattleMapLoaderSignImpl();
        }

        World world;
        try {
             world = BattleWorldManager.create(
                mapData,
                mapsDirectory,
                index
            );
        } catch (FileNotFoundException e) {
            return List.of("no_map_file");
        }

        BattleGameConfig config;
        try {
            config = parser.parse(mapData, world);
        } catch (Exception e) {
            return List.of("parser_error");
        }

        List<String> errors = evaluate(config);

        Bukkit.unloadWorld(world, false);

        return errors;
    }

    private List<String> evaluate(BattleGameConfig config) {
        List<String> errors = new ArrayList<>();

        for (BattleTeamConfig teamConfig : config.getTeamConfigs()) {
            if (teamConfig.getSpawn() == null) {
                errors.add("missing_team_%s_spawn".formatted(teamConfig.getId()));
            }
            if (teamConfig.getFlag() == null) {
                errors.add("missing_team_%s_flag".formatted(teamConfig.getId()));
            }
        }

        return errors;

    }

    private MapTestResults loadExistingResults(File outputFile) throws IOException {
        if (!outputFile.exists()) {
            outputFile.getParentFile().mkdir();
            return new MapTestResults();
        }

        return mapper.readValue(outputFile, MapTestResults.class);
    }

    private void saveResults(File outputFile, MapTestResults results) throws IOException {
        mapper.writeValue(outputFile, results);
        log.info("Saved testing results to {}", outputFile.getAbsoluteFile());
    }

    private void generateMapsJson(MapRepo repo, MapTestResults results, BattleOptionsInput.MapTesterOptions testerOptions) throws IOException {
        List<BattleMapData> maps = repo.getAll();
        for (BattleMapData map : maps) {
            List<String> errors = results.getErrors().get(map.getId());
            if (errors == null) {
                continue;
            }

            map.setErrors(errors);
            map.setFunctional(errors.isEmpty());
        }

        File file = new File(testerOptions.getOutputDir(), "maps_" + testerOptions.getRunId() + ".json");
        mapper.writeValue(file, maps);

        log.info("Saved a new maps.json file to {}", file.getAbsolutePath());
    }

    @Data
    private static class MapTestResults {
        private Map<Integer, List<String>> errors = new HashMap<>();
    }

}
