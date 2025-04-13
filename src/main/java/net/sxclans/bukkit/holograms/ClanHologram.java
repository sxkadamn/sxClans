package net.sxclans.bukkit.holograms;

import net.sxclans.common.clan.Clan;
import net.sxclans.bukkit.Depend;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ClanHologram extends BaseHologram<Clan> {

    public ClanHologram(JavaPlugin plugin, String id, String configPath,
                        Comparator<Clan> comparator) {
        super(plugin, id, configPath, new ClanContentProvider(configPath), new ClanDataProvider(comparator, configPath));
    }
}

class ClanContentProvider implements IHologramContentProvider<Clan> {
    private final HologramConfig config;
    private final String configPath;

    public ClanContentProvider(String configPath) {
        this.configPath = configPath;
        this.config = new HologramConfig(Depend.getInstance());
    }

    @Override
    public List<String> getHeader() {
        return config.getHeader(configPath);
    }

    @Override
    public List<String> getLines(List<Clan> clans) {
        List<String> lines = new ArrayList<>();
        List<String> configLines = config.getLines(configPath);

        for (int i = 0; i < Math.min(clans.size(), configLines.size()); i++) {
            Clan clan = clans.get(i);
            String line = configLines.get(i)
                    .replace("{name}", clan.getName())
                    .replace("{bank}", String.format("%.2f", clan.getBank()))
                    .replace("{level}", String.valueOf(clan.getLevel()))
                    .replace("{rubles}", String.format("%.2f", clan.getRubles()))
                    .replace("{leader}", clan.getLeader())
                    .replace("{position}", String.valueOf(i + 1));
            lines.add(line);
        }

        return lines;
    }

    @Override
    public List<String> getFooter() {
        return config.getFooter(configPath).stream()
                .map(footer -> footer.replace("{time}", LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))))
                .collect(Collectors.toList());
    }
}

class ClanDataProvider implements IHologramDataProvider<Clan> {
    private final Comparator<Clan> comparator;
    private final HologramConfig config;
    private final String configPath;

    public ClanDataProvider(Comparator<Clan> comparator, String configPath) {
        this.comparator = comparator;
        this.configPath = configPath;
        this.config = new HologramConfig(Depend.getInstance());
    }

    @Override
    public List<Clan> getData() {
        return Depend.getClanManage().getClans().values().stream()
                .sorted(comparator)
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public long getUpdateInterval() {
        return config.getUpdateInterval(configPath);
    }
}
