package net.sxclans.common.clan.base;

import net.sxclans.bukkit.Depend;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.war.manager.WarManager;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


public class FileBasedClanManager implements ClanBaseManager {
    private final File directory;
    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private final Map<String, String> playerToClan = new ConcurrentHashMap<>();
    private final ReentrantLock saveLock = new ReentrantLock();

    private final Map<String, String> warRequests = new ConcurrentHashMap<>();
    private final Map<String, WarManager> activeWars = new ConcurrentHashMap<>();

    public FileBasedClanManager(File directory) {
        this.directory = Objects.requireNonNull(directory, "Directory cannot be null");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        loadClans();
    }

    @Override
    public void loadClans() {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".clan"));
        if (files == null) return;

        Arrays.stream(files).parallel().forEach(file -> {
            try {
                Clan clan = readClanFromFile(file);
                if (clan != null) {
                    clans.put(clan.getName(), clan);
                    clan.getMembers().forEach(member -> playerToClan.put(member, clan.getName()));
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void loadClanFromFile(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
            Clan clan = (Clan) ois.readObject();
            clans.put(clan.getName(), clan);
            for (String member : clan.getMembers()) {
                playerToClan.put(member, clan.getName());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveClans() {
        clans.forEach((name, clan) -> saveClanToFile(new File(directory, name + ".clan"), clan));
    }

    @Override
    public void saveClan(Clan clan) {
        saveLock.lock();
        try {
            File tempFile = new File(directory, clan.getName() + ".tmp");
            File targetFile = new File(directory, clan.getName() + ".clan");

            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(tempFile.toPath()))) {
                oos.writeObject(clan);
            }

            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            clans.put(clan.getName(), clan);
            clan.getMembers().forEach(member -> playerToClan.put(member, clan.getName()));
        } catch (Exception ignored) {
        } finally {
            saveLock.unlock();
        }
    }

    private void saveClanToFile(File file, Clan clan) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file.toPath()))) {
                oos.writeObject(clan);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void removeClan(Clan clan) {
        saveLock.lock();
        try {
            File file = new File(directory, clan.getName() + ".clan");
            if (file.exists()) Files.delete(file.toPath());
            clans.remove(clan.getName());
            clan.getMembers().forEach(playerToClan::remove);
        } catch (Exception e) {
        } finally {
            saveLock.unlock();
        }
    }

    @Override
    public void removeMember(Clan clan, String member) {
        saveLock.lock();
        try {
            playerToClan.remove(clan.getMember(member));
        } catch (Exception ignored) {
        } finally {
            saveLock.unlock();
        }
    }

    @Override
    public Clan getClan(String name) {
        return clans.get(name);
    }

    @Override
    public Clan getOwnersClan(String leaderName) {
        return clans.values().stream()
                .filter(clan -> clan.getLeader().equals(leaderName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Clan getMembersClan(String memberName) {
        String clanName = playerToClan.get(memberName);
        if (clanName == null) {
            return null;
        }
        return clans.get(clanName);
    }

    public List<Clan> getTeams() {
        return Optional.ofNullable(directory.listFiles((dir, name) -> name.endsWith(".clan")))
                .stream()
                .flatMap(Arrays::stream)
                .map(this::readClanFromFile)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Clan readClanFromFile(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
            Clan clan = (Clan) ois.readObject();

            if (clan.getWarParticipants() == null) {
                clan.setWarParticipants(new ArrayList<>());
            }

            return clan;
        } catch (Exception e) {
            return null;
        }
    }

    public File getDirectory() {
        return directory;
    }

    public Map<String, Clan> getClans() {
        return new HashMap<>(clans);
    }

    public void addWarRequest(String senderClanName, String receiverClanName) {
        if (senderClanName == null || receiverClanName == null) {
            return;
        }

        Clan senderClan = clans.get(senderClanName);
        Clan receiverClan = clans.get(receiverClanName);

        if (senderClan == null || receiverClan == null) {
            return;
        }

        if (warRequests.containsKey(receiverClanName)) {
            return;
        }

        warRequests.put(receiverClanName, senderClanName);
    }

    @Override
    public void acceptWarRequest(String receiverClanName) {
        String senderClanName = warRequests.get(receiverClanName);
        if (senderClanName == null) return;

        Clan senderClan = clans.get(senderClanName);
        Clan receiverClan = clans.get(receiverClanName);

        if (senderClan == null || receiverClan == null) return;

        WarManager warManager = new WarManager(senderClan, receiverClan);
        setActiveWarManager(senderClan, receiverClan, warManager);
        warManager.startWarCountdown();

        senderClan.acceptWarRequest(receiverClan);

        warRequests.remove(receiverClanName);
    }

    @Override
    public void cancelWarRequest(String receiverClanName) {
        warRequests.remove(receiverClanName);
    }

    @Override
    public Map<String, String> getWarRequests() {
        return new HashMap<>(warRequests);
    }

    public Map<String, WarManager> getActiveWars() {
        return new HashMap<>(activeWars);
    }

    public void setActiveWarManager(Clan clanA, Clan clanB, WarManager warManager) {
        activeWars.entrySet().removeIf(entry -> entry.getValue() == warManager);

        clanA.getWarParticipants().forEach(p -> activeWars.put(p, warManager));
        clanB.getWarParticipants().forEach(p -> activeWars.put(p, warManager));
    }

    public WarManager getActiveWarManager(Player player) {
        return activeWars.get(player.getName());
    }

    public void clearWar(Clan clanA, Clan clanB) {
        activeWars.remove(clanA.getName());
        activeWars.remove(clanB.getName());
    }
}