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
import com.mcpvp.battle.map.manager.LocalMapManager;
import com.mcpvp.battle.map.manager.MapManager;
import com.mcpvp.battle.map.repo.LocalMapRepo;
import com.mcpvp.battle.map.repo.MapRepo;
import com.mcpvp.battle.match.BattleMatch;
import com.mcpvp.battle.match.BattleMatchManager;
import com.mcpvp.battle.options.BattleOptions;
import com.mcpvp.battle.options.BattleOptionsLoader;
import com.mcpvp.common.ProjectileManager;
import com.mcpvp.common.structure.StructureManager;
import com.mcpvp.common.visibility.VanillaVisibilityManager;
import com.mcpvp.common.visibility.VisibilityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

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
    private MapRepo mapRepo;
    private MapManager mapManager;
    private BattleGameManager gameManager;
    private BattleMatchManager matchManager;
    private BattleKitManager kitManager;
    private BattleMatch match;
    private ProjectileManager projectileManager;
    private StructureManager structureManager;
    private BattleInventoryManager inventoryManager;
    private VisibilityManager visibilityManager;

    public void load() throws IOException {
        this.options = new BattleOptions(plugin, BattleOptionsLoader.getInput(plugin, objectMapper));
        this.mapRepo = new LocalMapRepo(this.objectMapper, this.options.getMaps());
        this.mapRepo.init();
        this.mapManager = new LocalMapManager(this.plugin, this.mapRepo);
        this.gameManager = new BattleGameManager(this);
        this.matchManager = new BattleMatchManager(plugin, this, this.gameManager, this.mapManager);
        this.kitManager = new BattleKitManager(plugin, this);
        this.projectileManager = new ProjectileManager(plugin);
        this.structureManager = new StructureManager();
        this.inventoryManager = new BattleInventoryManager(plugin);
        this.inventoryManager.loadAll();
        this.visibilityManager = new VanillaVisibilityManager();

        BattleWorldManager.cleanUpWorlds();
    }

    public void start() {
        this.projectileManager.register();

        this.kitManager.getScoutDeathTagManager().register();
        this.kitManager.getNecroRevivalTagManager().register();

        if (getOptions().getMapTester().isEnabled()) {
            new BattleMapTester(objectMapper).run(getOptions().getMaps(), getOptions().getMapTester(), this.mapRepo);
            return;
        }

        this.match = this.matchManager.create();
        this.match.start();
    }

    public BattleGame getGame() {
        return getMatch().getCurrentGame();
    }

}
