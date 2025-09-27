package com.mcpvp.battle.kit;

import com.mcpvp.battle.Battle;
import com.mcpvp.battle.BattlePlugin;
import com.mcpvp.battle.game.BattleGame;
import com.mcpvp.battle.hud.HeadIndicator;
import com.mcpvp.battle.kit.item.FlagCompassItem;
import com.mcpvp.battle.kit.item.FoodItem;
import com.mcpvp.battle.team.BattleTeam;
import com.mcpvp.battle.team.BattleTeamManager;
import com.mcpvp.common.EasyLifecycle;
import com.mcpvp.common.item.ItemBuilder;
import com.mcpvp.common.item.ItemUtil;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.structure.Structure;
import com.mcpvp.common.structure.StructureViolation;
import com.mcpvp.common.task.ExpBarTask;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.nms.ActionbarUtil;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BattleKit extends Kit {

    @Nullable
    protected FoodItem foodItem;
    @Nullable
    protected BukkitTask expBarTask;

    public BattleKit(BattlePlugin plugin, Player player) {
        super(plugin, player);
    }

    public Battle getBattle() {
        return ((BattlePlugin) plugin).getBattle();
    }

    public BattleGame getGame() {
        return getBattle().getGame();
    }

    public BattleTeam getTeam() {
        return getBattle().getGame().getTeamManager().getTeam(getPlayer());
    }

    public void restoreFoodItem() {
        if (foodItem != null) {
            foodItem.increment(foodItem.getOriginal().getAmount());
        }
    }

    protected Set<Player> getEnemies() {
        BattleTeamManager teamManager = getBattle().getGame().getTeamManager();
        return teamManager.getNext(teamManager.getTeam(getPlayer())).getPlayers();
    }

    protected Set<Player> getTeammates() {
        BattleTeamManager teamManager = getBattle().getGame().getTeamManager();
        return teamManager.getTeam(getPlayer()).getPlayers();
    }

    protected boolean isTeammate(Player player) {
        return getGame().getTeamManager().isSameTeam(getPlayer(), player);
    }

    protected boolean isEnemy(Player player) {
        return !isTeammate(player) && getGame().getTeamManager().getTeam(player) != null;
    }

    protected boolean hasFlag() {
        BattleTeamManager teamManager = getBattle().getGame().getTeamManager();
        return teamManager.getTeams().stream().anyMatch(team -> team.getFlag().getCarrier() == getPlayer());
    }

    public boolean inSpawn() {
        BattleTeamManager teamManager = getBattle().getGame().getTeamManager();
        return teamManager.getTeam(getPlayer()).isInSpawn(getPlayer());
    }

    protected boolean placeStructure(Structure structure, Block center) {
        List<StructureViolation> violations = structure.place(center);
        if (!violations.isEmpty()) {
            ActionbarUtil.send(getPlayer(), C.warn(C.RED) + violations.get(0).getMessage());
            return false;
        } else {
            // Structure will be removed on kit destruction
            attach((EasyLifecycle) structure);
            return true;
        }
    }

    protected void animateExp(ExpBarTask task) {
        if (this.expBarTask != null) {
            this.expBarTask.cancel();
        }

        this.expBarTask = task.schedule(getPlugin());
        attach(this.expBarTask);
    }

    protected void attach(HeadIndicator indicator) {
        super.attach(indicator);
        indicator.apply();
    }

    @EventHandler
    public void onItemMove(InventoryCloseEvent event) {
        ((BattlePlugin) plugin).getBattle().getInventoryManager().save(this);
        // Ideally, we wouldn't save all the layouts here, but whatever
        ((BattlePlugin) plugin).getBattle().getInventoryManager().saveAll();
    }

    public class KitInventoryBuilder {

        private static final int INVENTORY_SIZE = 9 * 4;
        private final KitItem[] items = new KitItem[INVENTORY_SIZE];

        private int currentSlot = 0;

        public KitInventoryBuilder add(Material material) {
            return add(ItemBuilder.of(material));
        }

        public KitInventoryBuilder add(ItemBuilder builder) {
            items[currentSlot++] = autoAdjust(builder, false);
            return this;
        }

        public KitInventoryBuilder add(ItemBuilder builder, boolean restorable) {
            items[currentSlot++] = autoAdjust(builder, restorable);
            return this;
        }

        public KitInventoryBuilder add(KitItem item) {
            items[currentSlot++] = item;
            return this;
        }

        public KitInventoryBuilder addFood(int count) {
            FoodItem food = new FoodItem(
                BattleKit.this,
                ItemBuilder.of(Material.COOKED_BEEF).name("Food").amount(count).build()
            );
            foodItem = food;
            items[currentSlot++] = food;
            return this;
        }

        public KitInventoryBuilder addCompass(int slot) {
            items[slot] = new FlagCompassItem(BattleKit.this, getBattle().getGame());
            return this;
        }

        private KitItem autoAdjust(ItemBuilder itemBuilder, boolean restorable) {
            return new KitItem(
                BattleKit.this,
                itemBuilder.unbreakable().flag(ItemFlag.HIDE_UNBREAKABLE).build(),
                restorable
            );
        }

        public Map<Integer, KitItem> build() {
            Map<Integer, KitItem> map = new HashMap<>();
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) {
                    map.put(i, items[i]);
                }
            }
            return getBattle().getInventoryManager().applyLayout(BattleKit.this, map);
        }


    }

}
