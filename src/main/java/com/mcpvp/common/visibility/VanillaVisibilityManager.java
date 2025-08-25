package com.mcpvp.common.visibility;

import org.bukkit.entity.Player;

public class VanillaVisibilityManager implements VisibilityManager {

    @Override
    public void hide(Player observer, Player target) {
        observer.hidePlayer(target);
    }

    @Override
    public void show(Player observer, Player target) {
        observer.showPlayer(target);
    }

    @Override
    public boolean canSee(Player observer, Player target) {
        return observer.canSee(target);
    }

}
