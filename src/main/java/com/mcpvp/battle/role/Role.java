package com.mcpvp.battle.role;

public enum Role {
    ATTACK("A"),
    DEFENSE("D");

    private final String code;

    Role(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
