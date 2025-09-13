package com.mcpvp.battle;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.game.BattleGameManager;
import com.mcpvp.battle.kit.BattleInventoryManager;
import com.mcpvp.battle.kit.BattleKitManager;
import com.mcpvp.battle.map.BattleMapTester;
import com.mcpvp.battle.map.BattleWorldManager;
import com.mcpvp.battle.map.manager.MergingMapManager;
import com.mcpvp.battle.map.manager.BattleMapManager;
import com.mcpvp.battle.map.repo.BattleMapSource;
import com.mcpvp.battle.match.BattleMatch;
import com.mcpvp.battle.match.BattleMatchManager;
import com.mcpvp.battle.options.BattleOptions;
import com.mcpvp.battle.options.BattleOptionsLoader;
import com.mcpvp.common.structure.StructureManager;
import com.mcpvp.common.visibility.VanillaVisibilityManager;
import com.mcpvp.common.visibility.VisibilityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class Battle {

    private final BattlePlugin plugin;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(JsonParser.Feature.ALLOW_COMMENTS)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .registerModule(new JavaTimeModule())
        .setDateFormat(new StdDateFormat().withColonInTimeZone(true))
        .findAndRegisterModules();

    private BattleOptions options;
    private BattleMapManager mapManager;
    private BattleGameManager gameManager;
    private BattleMatchManager matchManager;
    private BattleKitManager kitManager;
    private BattleMatch match;
    private StructureManager structureManager;
    private BattleInventoryManager inventoryManager;
    private VisibilityManager visibilityManager;

    public void load() throws IOException {
        this.options = new BattleOptions(plugin, BattleOptionsLoader.getInput(plugin, objectMapper));
        this.mapManager = new MergingMapManager(this.plugin, options.getMaps(), loadMapRepos(this.options));
        this.gameManager = new BattleGameManager(this);
        this.matchManager = new BattleMatchManager(plugin, this, this.gameManager, this.mapManager);
        this.kitManager = new BattleKitManager(plugin, this);
        this.structureManager = new StructureManager();
        this.inventoryManager = new BattleInventoryManager(plugin);
        this.inventoryManager.loadAll();
        this.visibilityManager = new VanillaVisibilityManager();

        BattleWorldManager.cleanUpWorlds();
    }

    private List<BattleMapSource> loadMapRepos(BattleOptions options) {
        return options.getMaps().getSources().stream()
            .map(source -> BattleMapSource.from(objectMapper, source))
            .peek(BattleMapSource::init)
            .toList();
    }

    public void start() {
        this.kitManager.getScoutDeathTagManager().register();
        this.kitManager.getNecroRevivalTagManager().register();
        this.visibilityManager.init();

        if (getOptions().getMapTester().isEnabled()) {
            new BattleMapTester(objectMapper).run(
                getOptions().getMapTester(),
                BattleMapSource.from(objectMapper, getOptions().getMapTester().getMapSource()),
                getMapManager()
            );
            return;
        }

        this.match = this.matchManager.create();
        this.match.start();
    }

    public BattleGame getGame() {
        return getMatch().getCurrentGame();
    }

}
