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

/**
 * A single-use item with a fixed recharge time.
 */
public abstract class CooldownItem extends KitItem {

    private final Expiration cooldown = new Expiration();
    private final Duration cooldownTime;

    public CooldownItem(Kit kit, ItemStack itemStack, Duration cooldownTime) {
        super(kit, itemStack);
        this.cooldownTime = cooldownTime;
    }

    private float getRechargePercentage() {
        return this.cooldown.getCompletionPercent(this.cooldownTime);
    }

    protected boolean showExpRecharge() {
        return true;
    }

    protected boolean showDurabilityRecharge() {
        return false;
    }

    protected boolean autoUse() {
        return true;
    }

    @Override
    public void restore() {
        super.restore();
        this.cooldown.expireNow();
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (this.showExpRecharge() && this.isItem(this.kit.getPlayer().getItemInHand())) {
            this.kit.getPlayer().setExp(this.getRechargePercentage());
        }

        if (this.showDurabilityRecharge()) {
            this.modify(item -> item.durabilityPercent(this.getRechargePercentage()));
        }

        if (this.getRechargePercentage() >= 1 && this.isPlaceholder()) {
            this.restore();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!this.autoUse() || !this.shouldTrigger(event)) {
            return;
        }

        if (!this.isItem(event.getItem())) {
            return;
        }

        if (((BattleKit) this.kit).inSpawn()) {
            event.setCancelled(true);
            return;
        }

        if (!this.cooldown.isExpired()) {
            event.setCancelled(true);
            this.onFailedUse();
            return;
        }

        this.onUse(event);
        this.startCooldown();
    }

    protected boolean shouldTrigger(PlayerInteractEvent event) {
        return true;
    }

    protected void startCooldown() {
        this.cooldown.expireIn(this.cooldownTime);
    }

    protected void onFailedUse() {

    }

    protected abstract void onUse(PlayerInteractEvent event);

}
