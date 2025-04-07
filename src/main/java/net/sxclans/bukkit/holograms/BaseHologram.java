package net.sxclans.bukkit.holograms;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.lielibrary.bukkit.Plugin;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class BaseHologram<T> implements IHologram {
    protected final JavaPlugin plugin;
    protected final String id;
    protected final String configPath;
    protected final HologramConfig config;
    protected final IHologramContentProvider<T> contentProvider;
    protected final IHologramDataProvider<T> dataProvider;

    protected Hologram hologram;
    protected BukkitRunnable updateTask;

    public BaseHologram(JavaPlugin plugin, String id, String configPath,
                        IHologramContentProvider<T> contentProvider,
                        IHologramDataProvider<T> dataProvider) {
        this.plugin = plugin;
        this.id = id;
        this.configPath = configPath;
        this.config = new HologramConfig(plugin);
        this.contentProvider = contentProvider;
        this.dataProvider = dataProvider;
    }

    @Override
    public void create() {
        if (!isEnabled()) return;

        Location loc = getLocation();
        if (loc == null) {
            plugin.getLogger().warning("Invalid location for hologram " + id);
            return;
        }

        this.hologram = DHAPI.createHologram(id, loc, contentProvider.getHeader());
        startUpdater();
    }

    @Override
    public void update() {
        if (hologram == null) return;

        List<T> data = dataProvider.getData();
        List<String> content = new ArrayList<>(contentProvider.getHeader());

        for (int i = 0; i < data.size(); i++) {
            content.add(Plugin.getWithColor().hexToMinecraftColor(
                    contentProvider.getLines(data).get(i)
            ));
        }

        content.addAll(contentProvider.getFooter());
        DHAPI.setHologramLines(hologram, content);
    }

    @Override
    public void delete() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        if (hologram != null) {
            hologram.delete();
            hologram = null;
        }
    }

    @Override
    public void relocate(Location newLocation) {
        if (hologram != null) {
            hologram.setLocation(newLocation);
        }
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled(configPath);
    }

    @Override
    public Location getLocation() {
        return config.getLocation(configPath);
    }

    @Override
    public String getId() {
        return id;
    }

    protected void startUpdater() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };

        long interval = dataProvider.getUpdateInterval() * 20L;
        updateTask.runTaskTimer(plugin, 0, interval);
    }
}