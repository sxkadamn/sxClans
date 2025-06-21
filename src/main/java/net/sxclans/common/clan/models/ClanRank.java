package net.sxclans.common.clan.models;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.Serializable;

public enum ClanRank implements Serializable{
    LEADER("leader", 4),
    CO_LEADER("co_leader", 3),
    MODERATOR("moderator", 2),
    MEMBER("member", 1);

    private final String configKey;
    private final int power;
    private static FileConfiguration config;

    ClanRank(String configKey, int power) {
        this.configKey = configKey;
        this.power = power;
    }

    public int getPower() {
        return power;
    }

    public static void loadConfig(FileConfiguration configuration) {
        config = configuration;
    }

    public String getDisplayName() {
        if (config == null) return configKey;
        return config.getString("clan_ranks." + configKey, configKey);
    }

    public ClanRank getNextRank() {
        return switch (this) {
            case MEMBER -> MODERATOR;
            case MODERATOR -> CO_LEADER;
            case CO_LEADER -> LEADER;
            case LEADER -> null;
        };
    }
}