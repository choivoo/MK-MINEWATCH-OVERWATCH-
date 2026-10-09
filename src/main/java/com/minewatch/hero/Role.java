package com.minewatch.hero;

public enum Role {
    TANK("tank"), DAMAGE("damage"), SUPPORT("support");
    public final String key;
    Role(String key) { this.key = key; }
}
