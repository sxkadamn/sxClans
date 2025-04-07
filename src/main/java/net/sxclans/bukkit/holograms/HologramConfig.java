package net.sxclans.bukkit.holograms;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public class HologramConfig {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public HologramConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public Location getLocation(String path) {
        ConfigurationSection locSection = config.getConfigurationSection(path + ".location");
        if (locSection == null) return null;

        World world = Bukkit.getWorld(locSection.getString("world"));
        if (world == null) return null;

        return new Location(
                world,
                locSection.getDouble("x"),
                locSection.getDouble("y"),
                locSection.getDouble("z")
        );
    }

    public boolean isEnabled(String path) {
        return config.getBoolean(path + ".enabled", false);
    }

    public int getUpdateInterval(String path) {
        return config.getInt(path + ".update_interval_seconds", 60);
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