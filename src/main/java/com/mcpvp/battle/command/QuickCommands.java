package com.mcpvp.battle.command;

import com.mcpvp.battle.Battle;

public class QuickCommands {

    public static void registerAll(Battle battle) {
        new QuickCommand(battle, "gg", "Good Game!").all().register();
        new QuickCommand(battle, "gl", "Good luck!").all().register();
        new QuickCommand(battle, "md", "Medic!").loc().register();
        new QuickCommand(battle, "ty", "Thank you!").register();
        new QuickCommand(battle, "np", "No problem!").register();
        new QuickCommand(battle, "yw", "You're welcome!").register();
        new QuickCommand(battle, "c", "Careful!").register();
        new QuickCommand(battle, "s", "Sorry!").register();
        new QuickCommand(battle, "gj", "Good job!").register();
        new QuickCommand(battle, "d", "Defend, please!").register();
        new QuickCommand(battle, "omw", "On my way!").register();
        new QuickCommand(battle, "o", "Offend, please!").register();
        new QuickCommand(battle, "rp", "Recover, please!").register();
        new QuickCommand(battle, "h", "Help me!").loc().register();
        new QuickCommand(battle, "fm", "Follow me!").loc().register();
        new QuickCommand(battle, "wo", "Watch out!").loc().register();
        new QuickCommand(battle, "b", "Buffs, please!").loc().register();
        new QuickCommand(battle, "in", "Incoming!").loc().register();
        new QuickCommand(battle, "hf", "Have fun!").all().register();
        new QuickCommand(battle, "nw", "No worries!").register();
        new QuickCommand(battle, "wp", "Well played!").all().register();
        new QuickCommand(battle, "glhf", "Good luck, have fun!").all().register();
        new QuickCommand(battle, "belltower", "we need a fleet of soldiers to conquer the belltower").register();
        new QuickCommand(battle, "rmbr", "redslime may be responsible").all().register();
        new QuickCommand(battle, "ctf", "Capture the flag, please!").register();
        new QuickCommand(battle, "ns", "Nice shot!").all().register();
        new QuickCommand(battle, "lgt", "Let's go team!").register();
        new QuickCommand(battle, "bye", "bye.").register();
        new QuickCommand(battle, "ok", "ok.").register();
        new QuickCommand(battle, "udwm", "u don wan mess").register();
        new QuickCommand(battle, "ily", "I love you").register();
        new QuickCommand(battle, "bn", "Blame Nom!").all().register();
        new QuickCommand(battle, "mb", "My bad!").register();

        new QuickCommand(battle, "boa", "Boa!").register();
        new QuickCommand(battle, "valeu", "Valeu!").register();
        new QuickCommand(battle, "dr", "Drope a bandeira, por favor!").loc().register();
        new QuickCommand(battle, "se", "Segure a bandeira, por favor!").loc().register();
        new QuickCommand(battle, "re", "Retornem a bandeira, por favor!").loc().register();
    }

}
