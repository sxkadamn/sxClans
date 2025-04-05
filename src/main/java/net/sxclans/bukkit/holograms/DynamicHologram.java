package net.sxclans.bukkit.holograms;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.lielibrary.bukkit.Plugin;
import net.sxclans.bukkit.Depend;
import net.sxclans.common.clan.Clan;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DynamicHologram {
    private final Hologram hologram;
    private final HologramConfig config;
    private final String configPath;
    private final JavaPlugin plugin;

    public DynamicHologram(JavaPlugin plugin, String id, String configPath) {
        this.plugin = plugin;
        this.configPath = configPath;
        this.config = new HologramConfig(plugin);

        if (config.isEnabled(configPath)) {
            this.hologram = DHAPI.createHologram(
                    id,
                    config.getLocation(configPath),
                    config.getHeader(configPath)
            );
            startUpdater();
        } else {
            this.hologram = null;
        }
    }

    private void startUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        }.runTaskTimer(plugin, 0, config.getUpdateInterval(configPath) * 20L);
    }

    public void update() {
        if (hologram == null) return;

        List<Clan> topClans = getSortedClans();

        List<String> content = new ArrayList<>(config.getHeader(configPath));

        for (int i = 0; i < Math.min(topClans.size(), config.getLines(configPath).size()); i++) {
            Clan clan = topClans.get(i);
            String line = config.getLines(configPath).get(i)
                    .replace("{name}", clan.getName())
                    .replace("{bank}", String.format("%.2f", clan.getBank()))
                    .replace("{level}", String.format("%.1f", clan.getLevel()))
                    .replace("{leader}", clan.getLeader())
                    .replace("{position}", String.valueOf(i + 1));
            content.add(Plugin.getWithColor().hexToMinecraftColor(line));
        }

        config.getFooter(configPath).forEach(footer ->
                content.add(footer.replace("{time}", LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
                ));

        DHAPI.setHologramLines(hologram, content);
    }

    private List<Clan> getSortedClans() {
        Collection<Clan> clanValues = Depend.getClanManage().getClans().values();

        Comparator<Clan> comparator = configPath.contains("money")
                ? Comparator.comparingDouble(Clan::getBank).reversed()
                : Comparator.comparingDouble(Clan::getLevel).reversed();

        return clanValues.stream()
                .sorted(comparator)
                .limit(10)
                .collect(Collectors.toList());
    }

    public void delete() {
        if (hologram != null) hologram.delete();
    }
}