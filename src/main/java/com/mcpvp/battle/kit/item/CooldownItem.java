package com.mcpvp.battle.kit.item;

import com.mcpvp.battle.kit.BattleKit;
import com.mcpvp.common.event.TickEvent;
import com.mcpvp.common.kit.Kit;
import com.mcpvp.common.kit.KitItem;
import com.mcpvp.common.time.Duration;
import com.mcpvp.common.time.Expiration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public abstract class CooldownItem extends KitItem {

    private final Expiration cooldown = new Expiration();
    private final Duration cooldownTime;

    public CooldownItem(Kit kit, ItemStack itemStack, Duration cooldownTime) {
        super(kit, itemStack);
        this.cooldownTime = cooldownTime;
    }

    private float getRechargePercentage() {
        return Math.min(1, 1 - ((float) cooldown.getRemaining().ticks() / cooldownTime.ticks()));
    }

    protected boolean showExpRecharge() {
        return true;
    }

    protected boolean showDurabilityRecharge() {
        return false;
    }

    @Override
    public void restore() {
        super.restore();
        cooldown.expireNow();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (showExpRecharge() && isItem(kit.getPlayer().getItemInHand())) {
            kit.getPlayer().setExp(getRechargePercentage());
        }

        if (showDurabilityRecharge()) {
            modify(item -> item.durabilityPercent(getRechargePercentage()));
        }

        if (getRechargePercentage() >= 1 && isPlaceholder()) {
            restore();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isItem(event.getItem())) {
            return;
        }

        if (((BattleKit) kit).inSpawn()) {
            event.setCancelled(true);
            return;
        }

        if (!cooldown.isExpired()) {
            event.setCancelled(true);
            onFailedUse();
            return;
        }

        onUse(event);
        cooldown.expireIn(cooldownTime);
    }

    protected void onFailedUse() {

    }

    protected abstract void onUse(PlayerInteractEvent event);

}
