package net.sxclans.common.clan.war.manager;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import net.lielibrary.bukkit.Plugin;
import net.sxclans.bukkit.Depend;
import net.sxclans.common.clan.Clan;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WarManager {
    private final Clan clan1, clan2;
    private final Set<String> clan1Players = ConcurrentHashMap.newKeySet(), clan2Players = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> scoreboardTasks = new ConcurrentHashMap<>();
    private final Map<String, Location> playerOriginalLocations = new ConcurrentHashMap<>();
    private final FileConfiguration config;
    private final TabAPI tabAPI = TabAPI.getInstance();
    private int countdownId = -1, warTaskId = -1, scoreAnnounceId = -1, titleAnimId = -1, warTime = 0;
    private boolean warStarted = false;
    private final World warWorld;
    private final Location clan1Location, clan2Location;
    private static final List<Location> AVAILABLE_LOCATIONS = new ArrayList<>();
    private static final Map<String, WarManager> ACTIVE_WARS = new ConcurrentHashMap<>();
    private static File templateWorldDir;

    static {
        FileConfiguration config = Depend.getInstance().getConfig();
        List<Map<?, ?>> locations = config.getMapList("war_locations");
        for (Map<?, ?> loc : locations) {
            AVAILABLE_LOCATIONS.add(new Location(
                    null,
                    ((Number) loc.get("clan1_x")).doubleValue(),
                    ((Number) loc.get("clan1_y")).doubleValue(),
                    ((Number) loc.get("clan1_z")).doubleValue(),
                    ((Number) loc.get("clan1_yaw")).floatValue(),
                    ((Number) loc.get("clan1_pitch")).floatValue()
            ));
            AVAILABLE_LOCATIONS.add(new Location(
                    null,
                    ((Number) loc.get("clan2_x")).doubleValue(),
                    ((Number) loc.get("clan2_y")).doubleValue(),
                    ((Number) loc.get("clan2_z")).doubleValue(),
                    ((Number) loc.get("clan2_yaw")).floatValue(),
                    ((Number) loc.get("clan2_pitch")).floatValue()
            ));
        }
        String templateWorld = config.getString("war_template_world");
        templateWorldDir = new File(Bukkit.getWorldContainer(), templateWorld);
        if (!templateWorldDir.exists() || !templateWorldDir.isDirectory()) {
            throw new IllegalStateException("Template world directory '" + templateWorld + "' does not exist or is not a directory!");
        }
    }

    public WarManager(Clan c1, Clan c2) {
        this.clan1 = c1;
        this.clan2 = c2;
        this.config = Depend.getInstance().getConfig();
        this.clan1Players.addAll(c1.getWarParticipants());
        this.clan2Players.addAll(c2.getWarParticipants());
        this.scores.put(c1.getName(), 0);
        this.scores.put(c2.getName(), 0);

        String worldFolder = config.getString("war_world_folder", "wars");
        String worldName = "war_" + UUID.randomUUID().toString().replace("-", "");
        File worldDir = new File(Bukkit.getWorldContainer(), worldFolder + File.separator + worldName);

        try {
            copyWorldAsync(templateWorldDir, worldDir);
            this.warWorld = new WorldCreator(worldFolder + "/" + worldName).createWorld();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create war world: " + e.getMessage(), e);
        }

        int locationIndex = findAvailableLocationIndex();
        this.clan1Location = AVAILABLE_LOCATIONS.get(locationIndex * 2).clone();
        this.clan1Location.setWorld(warWorld);
        this.clan2Location = AVAILABLE_LOCATIONS.get(locationIndex * 2 + 1).clone();
        this.clan2Location.setWorld(warWorld);

        ACTIVE_WARS.put(worldName, this);
    }

    private void copyWorldAsync(File source, File destination) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    copyWorld(source, destination);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to copy world: " + e.getMessage(), e);
                }
            }
        }.runTaskAsynchronously(Depend.getInstance());
    }

    private void copyWorld(File source, File destination) throws IOException {
        if (!destination.exists()) {
            destination.mkdirs();
        }
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals("session.lock")) {
                    continue;
                }
                File newFile = new File(destination, file.getName());
                if (file.isDirectory()) {
                    copyWorld(file, newFile);
                } else {
                    try {
                        Files.copy(file.toPath(), newFile.toPath());
                    } catch (IOException e) {
                        Bukkit.getLogger().warning("Failed to copy file " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    private int findAvailableLocationIndex() {
        List<Integer> usedIndices = new ArrayList<>();
        for (WarManager war : ACTIVE_WARS.values()) {
            for (int i = 0; i < AVAILABLE_LOCATIONS.size() / 2; i++) {
                if (war.clan1Location.equals(AVAILABLE_LOCATIONS.get(i * 2)) ||
                        war.clan2Location.equals(AVAILABLE_LOCATIONS.get(i * 2 + 1))) {
                    usedIndices.add(i);
                }
            }
        }
        for (int i = 0; i < AVAILABLE_LOCATIONS.size() / 2; i++) {
            if (!usedIndices.contains(i)) return i;
        }
        throw new IllegalStateException("No available locations for war");
    }

    public void startWarCountdown() {
        int[] timeLeft = {config.getInt("war_start_delay")};
        countdownId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Depend.getInstance(), () -> {
            if (timeLeft[0]-- <= 0) {
                Bukkit.getScheduler().cancelTask(countdownId);
                warStarted = true;
                warTime = config.getInt("war_duration");
                config.getStringList("messages.war_start").forEach(this::notifyAll);

                for (String playerName : clan1Players) {
                    Player p = Bukkit.getPlayer(playerName);
                    if (p != null) playerOriginalLocations.put(playerName, p.getLocation());
                }
                for (String playerName : clan2Players) {
                    Player p = Bukkit.getPlayer(playerName);
                    if (p != null) playerOriginalLocations.put(playerName, p.getLocation());
                }

                teleportAll(clan1Players, clan1Location);
                teleportAll(clan2Players, clan2Location);

                for (String playerName : concat(clan1Players, clan2Players)) {
                    Player p = Bukkit.getPlayer(playerName);
                    if (p != null && tabAPI.getPlayer(p.getUniqueId()) != null) {
                        TabPlayer tabPlayer = tabAPI.getPlayer(p.getUniqueId());
                        Scoreboard sb = tabAPI.getScoreboardManager().createScoreboard("war", "", new ArrayList<>());
                        tabAPI.getScoreboardManager().showScoreboard(tabPlayer, sb);
                        scoreboardTasks.put(p.getUniqueId(), Bukkit.getScheduler().scheduleSyncRepeatingTask(Depend.getInstance(), () -> {
                            if (!warStarted) return;
                            sb.getLines().clear();
                            int a1 = countOnline(clan1Players), a2 = countOnline(clan2Players);
                            int max = Math.max(clan1Players.size(), clan2Players.size());
                            config.getStringList("scoreboard.lines").forEach(line -> {
                                String l = Plugin.getWithColor().hexToMinecraftColor(line
                                        .replace("{time_left}", String.valueOf(warTime))
                                        .replace("{score1}", String.valueOf(scores.get(clan1.getName())))
                                        .replace("{score2}", String.valueOf(scores.get(clan2.getName())))
                                        .replace("{alive1}", String.valueOf(a1))
                                        .replace("{alive2}", String.valueOf(a2))
                                        .replace("{dead1}", String.valueOf(clan1Players.size() - a1))
                                        .replace("{dead2}", String.valueOf(clan2Players.size() - a2))
                                        .replace("{max_score}", String.valueOf(max))
                                        .replace("{clan1_name}", clan1.getName())
                                        .replace("{clan2_name}", clan2.getName()));
                                sb.addLine(l);
                            });
                        }, 0, 20));
                    }
                }

                List<String> frames = config.getStringList("scoreboard.animations.title.frames");
                int interval = config.getInt("scoreboard.animations.title.interval", 10);
                AtomicInteger frame = new AtomicInteger(0);
                titleAnimId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Depend.getInstance(), () -> {
                    String t = Plugin.getWithColor().hexToMinecraftColor(frames.get(frame.getAndIncrement() % frames.size()));
                    for (String playerName : concat(clan1Players, clan2Players)) {
                        Player p = Bukkit.getPlayer(playerName);
                        if (p != null && tabAPI.getPlayer(p.getUniqueId()) != null) {
                            TabPlayer tab = tabAPI.getPlayer(p.getUniqueId());
                            Scoreboard sb = tabAPI.getScoreboardManager().getActiveScoreboard(tab);
                            if (sb != null) sb.setTitle(t);
                        }
                    }
                }, 0, interval);

                warTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Depend.getInstance(), () -> {
                    if (warTime-- <= 0) endWar();
                }, 0, 20);

                scoreAnnounceId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Depend.getInstance(), () -> {
                    notifyAll(config.getString("messages.score_notification", "{clan1} vs {clan2}")
                            .replace("{clan1}", clan1.getName() + " (" + scores.get(clan1.getName()) + ")")
                            .replace("{clan2}", clan2.getName() + " (" + scores.get(clan2.getName()) + ")")
                            .replace("{max_score}", String.valueOf(Math.max(clan1Players.size(), clan2Players.size()))));
                }, 0, 600);
            } else {
                notifyAll(config.getString("messages.war_start_notification", "&fStarting in {time}").replace("{time}", String.valueOf(timeLeft[0])));
            }
        }, 0, 20);
    }

    public void onPlayerDeath(Player p) {
        String playerName = p.getName();
        if (!clan1Players.contains(playerName) && !clan2Players.contains(playerName)) return;

        Player killer = p.getKiller();
        String killerClan;
        if (killer != null && (clan1Players.contains(killer.getName()) || clan2Players.contains(killer.getName()))) {
            killerClan = clan1Players.contains(killer.getName()) ? clan1.getName() : clan2.getName();
        }
        else {
            killerClan = clan1Players.contains(playerName)
                    ? clan2.getName()
                    : clan1.getName();
        }

        scores.merge(killerClan, 1, Integer::sum);
        clan1Players.remove(playerName);
        clan2Players.remove(playerName);
        notifyAll(config.getString("messages.player_eliminated", "&7Player from {clan_name} died! (+1 point to {killer_clan})")
                .replace("{clan_name}", clan1Players.contains(playerName) ? clan1.getName() : clan2.getName())
                .replace("{killer_clan}", killerClan));
        if (countOnline(clan1Players) == 0 || countOnline(clan2Players) == 0) endWar();
    }

    private void endWar() {
        warStarted = false;
        Bukkit.getScheduler().cancelTask(warTaskId);
        Bukkit.getScheduler().cancelTask(scoreAnnounceId);
        Bukkit.getScheduler().cancelTask(titleAnimId);
        scoreboardTasks.forEach((uuid, taskId) -> {
            Bukkit.getScheduler().cancelTask(taskId);
            TabPlayer tabPlayer = tabAPI.getPlayer(uuid);
            if (tabPlayer != null) {
                tabAPI.getScoreboardManager().resetScoreboard(tabPlayer);
            }
        });
        scoreboardTasks.clear();

        String result = scores.get(clan1.getName()) > scores.get(clan2.getName()) ? clan1.getName()
                : scores.get(clan2.getName()) > scores.get(clan1.getName()) ? clan2.getName() : "Draw";
        notifyAll(config.getString("messages.war_end", "&fWinner: {winner}").replace("{winner}", result));

        for (String playerName : concat(clan1Players, clan2Players)) {
            Player p = Bukkit.getPlayer(playerName);
            if (p != null) {
                Location originalLoc = playerOriginalLocations.get(playerName);
                if (originalLoc != null && originalLoc.getWorld() != null) {
                    p.teleport(originalLoc);
                } else {
                    p.performCommand("spawn");
                }
            }
        }
        playerOriginalLocations.clear();

        ACTIVE_WARS.remove(warWorld.getName());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (Bukkit.unloadWorld(warWorld, false)) {
                    String worldFolder = config.getString("war_world_folder", "wars");
                    File worldDir = new File(Bukkit.getWorldContainer(), worldFolder + File.separator + warWorld.getName());
                    deleteDirectory(worldDir);
                }
            }
        }.runTaskAsynchronously(Depend.getInstance());
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    private int countOnline(Set<String> playerNames) {
        int count = 0;
        for (String playerName : playerNames) {
            Player p = Bukkit.getPlayer(playerName);
            if (p != null && p.isOnline()) count++;
        }
        return count;
    }

    private void notifyAll(String message) {
        for (String playerName : concat(clan1Players, clan2Players)) {
            Player p = Bukkit.getPlayer(playerName);
            if (p != null) p.sendMessage(Plugin.getWithColor().hexToMinecraftColor(message));
        }
    }

    private void teleportAll(Set<String> playerNames, Location loc) {
        for (String playerName : playerNames) {
            Player p = Bukkit.getPlayer(playerName);
            if (p != null) p.teleport(loc);
        }
    }

    private Set<String> concat(Set<String> a, Set<String> b) {
        Set<String> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    public boolean isParticipant(Player player) {
        return clan1Players.contains(player.getName()) || clan2Players.contains(player.getName());
    }
}