package com.mcpvp.battle.options;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.common.JsonFile;
import lombok.experimental.Delegate;
import lombok.extern.log4j.Log4j2;

import java.io.File;

/**
 * Handles I/O for the config file.
 */
@Log4j2
public class BattleOptionsLoader {

    @Delegate
    private final JsonFile<BattleOptionsInput> file;

    public BattleOptionsLoader(
        BattlePlugin plugin,
        ObjectMapper mapper
    ) {
        this.file = new JsonFile<>(
            new File(plugin.getDataFolder(), "config.json"),
            mapper,
            new TypeReference<>() {},
            () -> BattleOptionsInput.builder().build()
        );
    }

}
