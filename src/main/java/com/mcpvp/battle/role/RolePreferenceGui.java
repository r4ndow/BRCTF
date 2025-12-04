package com.mcpvp.battle.role;

import com.mcpvp.battle.Battle;
import com.mcpvp.common.chat.C;
import com.mcpvp.common.event.EasyListener;
import com.mcpvp.common.item.ItemBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

@RequiredArgsConstructor
public class RolePreferenceGui implements EasyListener, InventoryHolder {

    private static final String TITLE = "Escolha uma função";
    private static final int ATTACK_SLOT = 2;
    private static final int EXIT_SLOT = 4;
    private static final int DEFENSE_SLOT = 6;

    private final Plugin plugin;
    private final Battle battle;
    private final RoleManager roleManager;

    public void init() {
        this.register();
    }

    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    public Inventory getInventory() {
        Inventory inv = Bukkit.createInventory(this, 9, TITLE);

        ItemStack attack = ItemBuilder.of(Material.GOLD_SWORD)
                .name(C.YELLOW + "Ataque")
                .desc(C.GRAY + "Roubar e capturar a bandeira inimiga!", 40)
                .enchant(Enchantment.DURABILITY, 10, true)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .flag(ItemFlag.HIDE_ENCHANTS)
                .build();

        ItemStack defense = ItemBuilder.of(Material.DIAMOND_CHESTPLATE)
                .name(C.AQUA + "Defesa")
                .desc(C.GRAY + "Defender e recuperar a bandeira do time!", 40)
                .enchant(Enchantment.DURABILITY, 10, true)
                .flag(ItemFlag.HIDE_ENCHANTS)
                .build();

        ItemStack exit = ItemBuilder.of(Material.BARRIER)
                .name(C.RED + "Fechar")
                //.desc("Fechar este menu sem alterar a função.", 40)
                .build();

        inv.setItem(ATTACK_SLOT, attack);
        inv.setItem(EXIT_SLOT, exit);
        inv.setItem(DEFENSE_SLOT, defense);

        return inv;
    }

    public void open(Player player) {
        player.openInventory(this.getInventory());
        player.playSound(player.getEyeLocation(), Sound.CLICK, 1.0f, 1.0f);
    }

    public void openForAllParticipants() {
        this.battle.getGame().getParticipants().forEach(this::open);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getHolder() != this) {
            return;
        }

        event.setCancelled(true);

        int slot = event.getRawSlot();
        Role chosen = null;

        if (slot == EXIT_SLOT) {
            player.playSound(player.getEyeLocation(), Sound.CLICK, 1.0f, 1.0f);
            player.closeInventory();
            return;
        }

        if (slot == ATTACK_SLOT) {
            chosen = Role.ATTACK;
            player.playSound(player.getEyeLocation(), Sound.CLICK, 1.0f, 1.0f);
        } else if (slot == DEFENSE_SLOT) {
            chosen = Role.DEFENSE;
            player.playSound(player.getEyeLocation(), Sound.CLICK, 1.0f, 1.0f);
        }

        if (chosen == null) {
            return;
        }

        this.roleManager.setRole(player, chosen, true);
        player.closeInventory();
        player.sendMessage(
                C.cmdPass()
                        + " Selected "
                        + C.WHITE
                        + chosen.name()
                        + C.GRAY
                        + " as your role."
        );
    }
}
