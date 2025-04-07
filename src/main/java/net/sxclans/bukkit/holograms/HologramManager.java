package net.sxclans.bukkit.holograms;

import net.sxclans.common.clan.Clan;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class HologramManager {
    private final JavaPlugin plugin;
    private final Map<String, IHologram> holograms = new HashMap<>();

    public HologramManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        Bukkit.getScheduler().runTaskLater(plugin, this::registerDefaultHolograms, 20L);
    }

    private void registerDefaultHolograms() {
        registerHologram("money_top", Comparator.comparingDouble(Clan::getBank).reversed());
        registerHologram("level_top", Comparator.comparingDouble(Clan::getLevel).reversed());
    }

    public void registerHologram(String id, Comparator<Clan> comparator) {
        String configPath = "holograms." + id;
        ClanHologram hologram = new ClanHologram(plugin, id, configPath, comparator);

        if (hologram.isEnabled()) {
            hologram.create();
            holograms.put(id, hologram);
        }
    }

    public void unregisterHologram(String id) {
        IHologram hologram = holograms.remove(id);
        if (hologram != null) {
            hologram.delete();
        }
    }

    public void reload() {
        holograms.values().forEach(IHologram::update);
    }

    public void disable() {
        holograms.values().forEach(IHologram::delete);
        holograms.clear();
    }

    public IHologram getHologram(String id) {
        return holograms.get(id);
    }
}