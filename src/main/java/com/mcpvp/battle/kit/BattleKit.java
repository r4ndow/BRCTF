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
        return ((BattlePlugin) this.plugin).getBattle();
    }

    public BattleGame getGame() {
        return this.getBattle().getGame();
    }

    public BattleTeam getTeam() {
        return this.getBattle().getGame().getTeamManager().getTeam(this.getPlayer());
    }

    public void restoreFoodItem() {
        if (this.foodItem != null) {
            this.foodItem.increment(this.foodItem.getOriginal().getAmount());
        }
    }

    protected Set<Player> getEnemies() {
        BattleTeamManager teamManager = this.getBattle().getGame().getTeamManager();
        return teamManager.getNext(teamManager.getTeam(this.getPlayer())).getPlayers();
    }

    protected Set<Player> getTeammates() {
        BattleTeamManager teamManager = this.getBattle().getGame().getTeamManager();
        return teamManager.getTeam(this.getPlayer()).getPlayers();
    }

    protected boolean isTeammate(Player player) {
        return this.getGame().getTeamManager().isSameTeam(this.getPlayer(), player);
    }

    protected boolean isEnemy(Player player) {
        return !this.isTeammate(player) && this.getGame().getTeamManager().getTeam(player) != null;
    }

    protected boolean hasFlag() {
        BattleTeamManager teamManager = this.getBattle().getGame().getTeamManager();
        return teamManager.getTeams().stream().anyMatch(team -> team.getFlag().getCarrier() == this.getPlayer());
    }

    public boolean inSpawn() {
        BattleTeamManager teamManager = this.getBattle().getGame().getTeamManager();
        return teamManager.getTeam(this.getPlayer()).isInSpawn(this.getPlayer());
    }

    protected boolean placeStructure(Structure structure, Block center) {
        List<StructureViolation> violations = structure.place(center);
        if (!violations.isEmpty()) {
            ActionbarUtil.send(this.getPlayer(), C.warn(C.RED) + violations.get(0).getMessage());
            return false;
        } else {
            // Structure will be removed on kit destruction
            this.attach((EasyLifecycle) structure);
            return true;
        }
    }

    protected void animateExp(ExpBarTask task) {
        if (this.expBarTask != null) {
            this.expBarTask.cancel();
        }

        this.expBarTask = task.schedule(this.getPlugin());
        this.attach(this.expBarTask);
    }

    protected void attach(HeadIndicator indicator) {
        super.attach(indicator);
        indicator.apply();
    }

    @EventHandler
    public void onItemMove(InventoryCloseEvent event) {
        ((BattlePlugin) this.plugin).getBattle().getInventoryManager().save(this);
        // Ideally, we wouldn't save all the layouts here, but whatever
        ((BattlePlugin) this.plugin).getBattle().getInventoryManager().saveAll();
    }

    public class KitInventoryBuilder {

        private static final int INVENTORY_SIZE = 9 * 4;
        private final KitItem[] items = new KitItem[INVENTORY_SIZE];

        private int currentSlot = 0;

        public KitInventoryBuilder add(Material material) {
            return this.add(ItemBuilder.of(material));
        }

        public KitInventoryBuilder add(ItemBuilder builder) {
            this.items[this.currentSlot++] = this.autoAdjust(builder, false);
            return this;
        }

        public KitInventoryBuilder add(ItemBuilder builder, boolean restorable) {
            this.items[this.currentSlot++] = this.autoAdjust(builder, restorable);
            return this;
        }

        public KitInventoryBuilder add(KitItem item) {
            this.items[this.currentSlot++] = item;
            return this;
        }

        public KitInventoryBuilder addFood(int count) {
            FoodItem food = new FoodItem(
                BattleKit.this,
                ItemBuilder.of(Material.COOKED_BEEF).name("Food").amount(count).build()
            );
            BattleKit.this.foodItem = food;
            this.items[this.currentSlot++] = food;
            return this;
        }

        public KitInventoryBuilder addCompass(int slot) {
            this.items[slot] = new FlagCompassItem(BattleKit.this, BattleKit.this.getBattle().getGame());
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
            for (int i = 0; i < this.items.length; i++) {
                if (this.items[i] != null) {
                    map.put(i, this.items[i]);
                }
            }
            return BattleKit.this.getBattle().getInventoryManager().applyLayout(BattleKit.this, map);
        }


    }

}
