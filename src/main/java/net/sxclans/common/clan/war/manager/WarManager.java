package net.sxclans.common.clan.war.manager;

import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import net.lielibrary.bukkit.Plugin;
import net.sxclans.bukkit.Depend;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.war.arena.Arena;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Score;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class WarManager {

    private final Clan clan1, clan2;
    private final Set<String> clan1Players = ConcurrentHashMap.newKeySet(), clan2Players = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> scores = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> scoreboardTasks = new ConcurrentHashMap<>();
    private final Map<String, Location> playerOriginalLocations = new ConcurrentHashMap<>();
    private final FileConfiguration config;
    private final TabAPI tabAPI = TabAPI.getInstance();

    private static final Map<String, WarManager> ACTIVE_WARS = new ConcurrentHashMap<>();

    private final Location clan1Location, clan2Location;
    private final int maxScore;
    private int countdownId = -1, scoreAnnounceId = -1, titleAnimId = -1, taskId = -1;
    private boolean warStarted = false;
    private int warTime = 0;
    private final Arena arena;
    private final Map<UUID, Scoreboard> playerScoreboards = new ConcurrentHashMap<>();

    public WarManager(Clan c1, Clan c2, Arena arena) {
        this.clan1 = c1;
        this.clan2 = c2;
        this.arena = arena;
        this.config = Depend.getInstance().getConfig();
        this.clan1Players.addAll(c1.getWarParticipants());
        this.clan2Players.addAll(c2.getWarParticipants());
        this.scores.put(c1.getName(), 0);
        this.scores.put(c2.getName(), 0);
        this.clan1Location = arena.getClan1Spawns();
        this.clan2Location = arena.getClan2Spawns();
        this.maxScore = Depend.getInstance().getConfig().getInt("war_maxScores");
        ACTIVE_WARS.put(arena.getName(), this);
    }

    public void startWarCountdown() {
        int[] timeLeft = {config.getInt("war_start_delay")};
        countdownId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Depend.getInstance(), () -> {
            if (timeLeft[0]-- <= 0) {
                Bukkit.getScheduler().cancelTask(countdownId);
                startWar();
            } else {
                notifyAll(config.getString("messages.war_start_notification")
                        .replace("{time}", String.valueOf(timeLeft[0])));
            }
        }, 0, 20);
    }

    private void startWar() {
        warStarted = true;
        arena.setOpen(false);
        warTime = config.getInt("war_duration");
        config.getStringList("messages.war_start").forEach(this::notifyAll);

        Stream.concat(clan1Players.stream(), clan2Players.stream()).forEach(name -> {
            Player p = Bukkit.getPlayer(name);
            if (p != null) playerOriginalLocations.put(name, p.getLocation());
        });

        teleportAll(clan1Players, clan1Location);
        teleportAll(clan2Players, clan2Location);

        List<String> lines = config.getStringList("scoreboard.lines");
        List<String> titleFrames = config.getStringList("scoreboard.animations.title.frames");
        int titleInterval = config.getInt("scoreboard.animations.title.interval", 10);
        AtomicInteger titleIndex = new AtomicInteger(0);

        Stream.concat(clan1Players.stream(), clan2Players.stream()).forEach(name -> {
            Player p = Bukkit.getPlayer(name);
            if (p != null && tabAPI.getPlayer(p.getUniqueId()) != null) {
                TabPlayer tabPlayer = tabAPI.getPlayer(p.getUniqueId());
                Scoreboard sb = tabAPI.getScoreboardManager().createScoreboard("war", "", new ArrayList<>());
                tabAPI.getScoreboardManager().showScoreboard(tabPlayer, sb);
                playerScoreboards.put(p.getUniqueId(), sb);

                taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Depend.getInstance(), () -> {
                    if (!warStarted) return;

                    Player onlinePlayer = Bukkit.getPlayer(name);
                    if (onlinePlayer == null) return;

                    if (tabPlayer == null) return;

                    int a1 = countOnline(clan1Players), a2 = countOnline(clan2Players);
                    int max = Math.max(clan1Players.size(), clan2Players.size());

                    Scoreboard scoreboard = playerScoreboards.get(p.getUniqueId());
                    if (scoreboard == null) return;

                    scoreboard.getLines().clear();
                    lines.forEach(line -> scoreboard.addLine(Plugin.getWithColor().hexToMinecraftColor(line
                            .replace("{time_left}", String.valueOf(warTime))
                            .replace("{score1}", String.valueOf(scores.get(clan1.getName())))
                            .replace("{score2}", String.valueOf(scores.get(clan2.getName())))
                            .replace("{alive1}", String.valueOf(a1))
                            .replace("{alive2}", String.valueOf(a2))
                            .replace("{dead1}", String.valueOf(clan1Players.size() - a1))
                            .replace("{dead2}", String.valueOf(clan2Players.size() - a2))
                            .replace("{max_score}", String.valueOf(max))
                            .replace("{clan1_name}", clan1.getName())
                            .replace("{clan2_name}", clan2.getName()))));
                    warTime--;
                }, 0, 20);

                scoreboardTasks.put(p.getUniqueId(), taskId);
            }
        });

        titleAnimId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Depend.getInstance(), () -> {
            String frame = Plugin.getWithColor().hexToMinecraftColor(titleFrames.get(titleIndex.getAndIncrement() % titleFrames.size()));
            playerScoreboards.forEach((uuid, sb) -> {
                TabPlayer tab = tabAPI.getPlayer(uuid);
                if (tab != null && sb != null) sb.setTitle(frame);
            });
        }, 0, titleInterval);

        scoreAnnounceId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Depend.getInstance(), () ->
                notifyAll(config.getString("messages.score_notification", "{clan1} vs {clan2}")
                        .replace("{clan1}", clan1.getName() + " (" + scores.get(clan1.getName()) + ")")
                        .replace("{clan2}", clan2.getName() + " (" + scores.get(clan2.getName()) + ")")
                        .replace("{max_score}", String.valueOf(Math.max(clan1Players.size(), clan2Players.size())))
                ), 0, 600);
    }

    public void onPlayerDeath(Player p) {
        String name = p.getName();
        if (!clan1Players.contains(name) && !clan2Players.contains(name)) return;

        String victimClan = clan1Players.contains(name) ? clan1.getName() : clan2.getName();

        String killerClan = Optional.ofNullable(p.getKiller())
                .map(Player::getName)
                .filter(n -> clan1Players.contains(n) || clan2Players.contains(n))
                .map(n -> clan1Players.contains(n) ? clan1.getName() : clan2.getName())
                .orElse(clan1Players.contains(name) ? clan2.getName() : clan1.getName());

        scores.merge(killerClan, 1, Integer::sum);

        clan1Players.remove(name);
        clan2Players.remove(name);

        notifyAll(config.getString("messages.player_eliminated")
                .replace("{clan_name}", victimClan)
                .replace("{killer_clan}", killerClan));

        if (scores.get(clan1.getName()) == maxScore
                || scores.get(clan2.getName()) == maxScore) {
            endWar();
        }
    }

    private void endWar() {
        warStarted = false;
        arena.setOpen(true);
        Bukkit.getScheduler().cancelTask(scoreAnnounceId);
        Bukkit.getScheduler().cancelTask(titleAnimId);
        Bukkit.getScheduler().cancelTask(taskId);

        scoreboardTasks.forEach((uuid, id) -> {
            Bukkit.getScheduler().cancelTask(id);
            Optional.ofNullable(tabAPI.getPlayer(uuid)).ifPresent(tab -> tabAPI.getScoreboardManager().resetScoreboard(tab));
        });
        scoreboardTasks.clear();
        playerScoreboards.clear();

        clan1.setInWar(false);
        clan2.setInWar(false);

        String winner = scores.get(clan1.getName()) > scores.get(clan2.getName()) ? clan1.getName()
                : scores.get(clan2.getName()) > scores.get(clan1.getName()) ? clan2.getName() : "Draw";
        notifyAll(config.getString("messages.war_end").replace("{winner}", winner));

        Stream.concat(clan1Players.stream(), clan2Players.stream()).forEach(name -> {
            Player p = Bukkit.getPlayer(name);
            if (p != null) {
                Location loc = playerOriginalLocations.getOrDefault(name, null);
                if (loc != null && loc.getWorld() != null) p.teleport(loc);
                else p.performCommand("spawn");
            }
        });
        playerOriginalLocations.clear();
        ACTIVE_WARS.remove(arena.getName());
    }

    private void notifyAll(String message) {
        Stream.concat(clan1Players.stream(), clan2Players.stream()).forEach(name -> {
            Player p = Bukkit.getPlayer(name);
            if (p != null) p.sendMessage(Plugin.getWithColor().hexToMinecraftColor(message));
        });
    }

    private void teleportAll(Set<String> names, Location loc) {
        names.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> p.teleport(loc));
    }

    private int countOnline(Set<String> names) {
        return (int) names.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).filter(Player::isOnline).count();
    }

    public boolean isParticipant(Player p) {
        return clan1Players.contains(p.getName()) || clan2Players.contains(p.getName());
    }
}
