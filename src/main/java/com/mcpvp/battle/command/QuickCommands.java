package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;

public class QuickCommands {

    public static void registerAll(Battle battle) {
        new QuickCommand(battle, "gg", "Good Game!").all();
        new QuickCommand(battle, "gl", "Good luck!").all();
        new QuickCommand(battle, "md", "Medic!").loc();
        new QuickCommand(battle, "ty", "Thank you!");
        new QuickCommand(battle, "np", "No problem!");
        new QuickCommand(battle, "yw", "You're welcome!");
        new QuickCommand(battle, "c", "Careful!");
        new QuickCommand(battle, "s", "Sorry!");
        new QuickCommand(battle, "gj", "Good job!");
        new QuickCommand(battle, "d", "Defend, please!");
        new QuickCommand(battle, "omw", "On my way!");
        new QuickCommand(battle, "o", "Offend, please!");
        new QuickCommand(battle, "rp", "Recover, please!");
        new QuickCommand(battle, "h", "Help me!").loc();
        new QuickCommand(battle, "fm", "Follow me!").loc();
        new QuickCommand(battle, "wo", "Watch out!").loc();
        new QuickCommand(battle, "b", "Buffs, please!").loc();
        new QuickCommand(battle, "in", "Incoming!").loc();
        new QuickCommand(battle, "hf", "Have fun!").all();
        new QuickCommand(battle, "nw", "No worries!");
        new QuickCommand(battle, "wp", "Well played!").all();
        new QuickCommand(battle, "glhf", "Good luck, have fun!").all();
        new QuickCommand(battle, "belltower", "we need a fleet of soldiers to conquer the belltower");
        new QuickCommand(battle, "rmbr", "redslime may be responsible").all();
        new QuickCommand(battle, "ctf", "Capture the flag, please!");
        new QuickCommand(battle, "ns", "Nice shot!").all();
        new QuickCommand(battle, "lgt", "Let's go team!");
        new QuickCommand(battle, "bye", "bye.");
        new QuickCommand(battle, "ok", "ok.");
        new QuickCommand(battle, "udwm", "u don wan mess");
        new QuickCommand(battle, "ily", "I love you");
        new QuickCommand(battle, "bn", "Blame Nom!").all();
        new QuickCommand(battle, "mb", "My bad!");
    }

}
