package net.sxclans.bukkit.holograms;

import org.bukkit.plugin.java.JavaPlugin;

public class HologramManager {
    private final DynamicHologram moneyTop;
    private final DynamicHologram levelTop;

    public HologramManager(JavaPlugin plugin) {
        this.moneyTop = new DynamicHologram(plugin, "money_top", "holograms.money_top");
        this.levelTop = new DynamicHologram(plugin, "level_top", "holograms.level_top");
    }

    public void reload() {
        moneyTop.update();
        levelTop.update();
    }

    public void disable() {
        moneyTop.delete();
        levelTop.delete();
    }
}
