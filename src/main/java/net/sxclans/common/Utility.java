package net.sxclans.common;

import net.sxclans.bukkit.Depend;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {
    public static void playConfiguredSounds(Player player, String string2) {
        Depend.getInstance().getConfig().getStringList(string2).forEach(string -> {
            String[] stringArray = string.split(":");
            if (stringArray.length == 3) {
                try {
                    Sound sound = Sound.valueOf(stringArray[0]);
                    float f = Float.parseFloat(stringArray[1]);
                    float f2 = Float.parseFloat(stringArray[2]);
                    player.playSound(player.getLocation(), sound, f, f2);
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    Depend.getInstance().getLogger().warning(Depend.getInstance().getConfig().getString("messages.invalid_sound_configuration").replace("{soundData}", (CharSequence)string));
                }
            } else {
                Depend.getInstance().getLogger().warning(Depend.getInstance().getConfig().getString("messages.invalid_sound_format").replace("{soundData}", (CharSequence)string));
            }
        });
    }

    public static void checkPlugin(String string) {
        if (!Bukkit.getServer().getPluginManager().isPluginEnabled(string)) {
            Bukkit.getLogger().severe(Depend.getInstance().getConfig().getString("messages.plugin_not_enabled").replace("{pluginName}", string));
            Bukkit.getServer().getPluginManager().disablePlugin(Depend.getInstance());
        }
    }

    public static void serializeLocation(YamlConfiguration config, String path, Location loc) {
        if (loc == null || loc.getWorld() == null) return;

        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
    }

    public static Location deserializeLocation(YamlConfiguration config, String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return null;

        String worldName = section.getString("world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");

        return new Location(world, x, y, z);
    }

    public static Float getVersion() {
        try {
            String string = Bukkit.getVersion();
            Pattern pattern = Pattern.compile("\\(MC: ([0-9]+\\.[0-9]+)");
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                return Float.valueOf(Float.parseFloat(matcher.group(1)));
            }
        }
        catch (NumberFormatException numberFormatException) {
            throw new RuntimeException(numberFormatException);
        }
        return Float.valueOf(0.0f);
    }

    public static void saveSafely(String name, Runnable action) {
        try {
            action.run();
            Bukkit.getLogger().info("Successfully saved " + name + ".");
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to save " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void cancelAllTasks() {
        Bukkit.getScheduler().cancelTasks(Depend.getInstance());
        Bukkit.getLogger().info("All scheduled tasks cancelled.");
    }
}
