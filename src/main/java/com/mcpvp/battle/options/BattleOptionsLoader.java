package com.mcpvp.battle.options;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.battle.BattlePlugin;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;

/**
 * Handles I/O for the config file.
 */
@Log4j2
public class BattleOptionsLoader {

    /**
     * Loads the raw input file, which will be converted into {@link BattleOptions}.
     *
     * @param plugin The plugin to load the data for.
     * @return The deserialized input.
     * @throws IOException If something goes wrong loading the files.
     */
    public static BattleOptionsInput getInput(BattlePlugin plugin, ObjectMapper mapper) throws IOException {
        if (!plugin.getDataFolder().exists()) {
            if (!plugin.getDataFolder().mkdir()) {
                throw new IOException("Unable to make the ctf plugin folder at: " + plugin.getDataFolder() + ". Please create this folder manually.");
            }
        }

        File config = new File(plugin.getDataFolder(), "config.json");
        if (!config.exists()) {
            log.info("No file found at {}. Writing defaults.", config);
            mapper.writeValue(config, BattleOptionsInput.builder().build());
        }

        try {
            return mapper.readValue(config, BattleOptionsInput.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read config located at " + config + ". If this persists, you delete this file to have it regenerated for you.", e);
        }
    }


}
