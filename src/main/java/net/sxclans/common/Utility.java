package net.sxclans.common;

import net.sxclans.bukkit.Depend;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
}
