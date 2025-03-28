package net.sxclans.common.clan.base;

import net.sxclans.common.clan.Clan;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class FileBasedClanManager implements ClanBaseManager{
    private final File directory;
    private final Map<String, Clan> clans;
    private final Map<String, String> playerToClan;

    public FileBasedClanManager(File directory) {
        this.directory = Objects.requireNonNull(directory, "Directory cannot be null");
        this.clans = new HashMap<>();
        this.playerToClan = new HashMap<>();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        loadClans();
    }

    public void loadClans() {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".clan"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            loadClanFromFile(file);
        }
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

    public void saveClans() {
        clans.forEach((name, clan) -> saveClanToFile(new File(directory, name + ".clan"), clan));
    }

    public void saveClan(Clan clan) {
        File file = new File(directory, clan.getName() + ".clan");
        saveClanToFile(file, clan);
        clans.put(clan.getName(), clan);

        List<String> currentMembers = clan.getMembers();
        playerToClan.entrySet().removeIf(entry -> entry.getValue().equals(clan.getName())
                && !currentMembers.contains(entry.getKey()));
        for (String member : currentMembers) {
            playerToClan.put(member, clan.getName());
        }

        saveClans();
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

    public void removeClan(Clan clan) {
        File file = new File(directory, clan.getName() + ".clan");
        if (file.exists()) {
            file.delete();
        }
        for (String member : clan.getMembers()) {
            playerToClan.remove(member);
        }
        clans.remove(clan.getName());
        saveClans();
    }

    public Clan getClan(String name) {
        return clans.get(name);
    }

    public Clan getOwnersClan(String leaderName) {
        return clans.values().stream()
                .filter(clan -> clan.getLeader().equals(leaderName))
                .findFirst()
                .orElse(null);
    }

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
            return (Clan) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public File getDirectory() {
        return directory;
    }

    public HashMap<String, Clan> getClans() {
        return (HashMap<String, Clan>) Collections.unmodifiableMap(clans);
    }
}
