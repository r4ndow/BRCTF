package com.mcpvp.battle.map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BattleMapCategory {
    DEFAULT("Unclassified CTF Maps"),
    MCPVP("Maps from MCPVP era"),
    CTF2015("Maps from 2015"),
    CTF2016("Maps from 2016"),
    CTF2017("Maps from 2017"),
    CTF2018("Maps from 2018"),
    CTF2019("Maps from 2019"),
    CTF2020("Maps from 2020"),
    WAVE1("Maps from Wave I"),
    WAVE2("Maps from Wave II"),
    WAVE3("Maps from Wave III"),
    WAVE4("Maps from Wave IV"),
    WAVE5("Maps from Wave V"),
    WAVE6("Maps from Wave VI"),
    WAVE7("Maps from Wave VII"),
    WAVE8("Maps from Wave VIII"),
    WAVE9("Maps from Wave IX"),
    WAVE10("Maps from Wave X"),
    WAVE11("Maps from Wave XI"),
    WAVE12("Maps from Wave XII"),
    WAVE13("Maps from Wave XIII"),
    WAVE14("Maps from Wave XIV"),
    WAVE15("Maps from Wave XV"),
    WAVE16("Maps from Wave XVI"),
    WAVE17("Maps from Wave XVII"),
    WAVE18("Maps from Wave XVIII"),
    WAVE19("Maps from Wave XIX"),
    WAVE20("Maps from Wave XX"),
    WAVE10TEST("Rejected Maps from Wave X"),
    WAVE11TEST("Rejected Maps from Wave XI"),
    WAVE12TEST("Rejected Maps from Wave XII"),
    WAVE13TEST("Rejected Maps from Wave XIII"),
    WAVE14TEST("Rejected Maps from Wave XIV"),
    WAVE15TEST("Rejected Maps from Wave XV"),
    WAVE16TEST("Rejected Maps from Wave XVI"),
    WAVE17TEST("Rejected Maps from Wave XVII"),
    WAVE18TEST("Rejected Maps from Wave XVIII"),
    WAVE19TEST("Rejected Maps from Wave XIX"),
    WAVE20TEST("Rejected Maps from Wave XX"),
    VAULT("Maps from MCPVP's Vault"),
    SPECIAL("Special Gamemodes/Events"),
    SPOOKY("Spooky Halloween Maps"),
    CHRISTMAS("Festive Christmas Maps"),
    LOVE("Valentine Day Maps"),
    EASTER("Easter Maps"),
    ZONES("Zone Control Maps"),
    DELIVERY("Delivery Maps"),
    FOURTEAMS("4-Teams CTF Maps"),
    XD("April's Fool Maps"),
    MLG("MLG Maps"),
    VARIATION("Variation Maps"),
    ORIGINAL("Original Maps from 2011"),
    UNRELEASED("Unreleased Maps"),
    MEME("Various Memes");

    private final String desc;

}
