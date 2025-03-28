package net.sxclans.common.clan.models;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.Serializable;

public enum ClanRank implements Serializable {
    LEADER("leader"),
    CO_LEADER("co_leader"),
    MODERATOR("moderator"),
    MEMBER("member");

    private final String configKey;
    private static FileConfiguration config;

    ClanRank(String configKey) {
        this.configKey = configKey;
    }

    public static void loadConfig(FileConfiguration configuration) {
        config = configuration;
    }

    public String getDisplayName() {
        if (config == null) {
            return configKey;
        }
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