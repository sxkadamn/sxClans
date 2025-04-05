package net.sxclans.bukkit.holograms;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class HologramConfig {
    private final FileConfiguration config;

    public HologramConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public Location getLocation(String path) {
        ConfigurationSection locSection = config.getConfigurationSection(path + ".location");
        World world = Bukkit.getWorld(locSection.getString("world"));
        return new Location(
                world,
                locSection.getDouble("x"),
                locSection.getDouble("y"),
                locSection.getDouble("z")
        );
    }

    public boolean isEnabled(String path) {
        return config.getBoolean(path + ".enabled");
    }

    public int getUpdateInterval(String path) {
        return config.getInt(path + ".update_interval_seconds");
    }

    public List<String> getHeader(String path) {
        return config.getStringList(path + ".header");
    }

    public List<String> getLines(String path) {
        return config.getStringList(path + ".lines");
    }

    public List<String> getFooter(String path) {
        return config.getStringList(path + ".footer");
    }
}
