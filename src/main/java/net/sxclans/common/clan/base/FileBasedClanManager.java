package net.sxclans.common.clan.base;

import net.sxclans.common.clan.Clan;

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

public class FileBasedClanManager implements ClanBaseManager{
    private final File directory;
    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private final Map<String, String> playerToClan = new ConcurrentHashMap<>();
    private final ReentrantLock saveLock = new ReentrantLock();

    public FileBasedClanManager(File directory) {
        this.directory = Objects.requireNonNull(directory, "Directory cannot be null");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        loadClans();
    }

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
            } catch (Exception e) {
                System.err.println("Ошибка загрузки клана из " + file.getName() + ": " + e.getMessage());
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

    public void saveClans() {
        clans.forEach((name, clan) -> saveClanToFile(new File(directory, name + ".clan"), clan));
    }

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
        } catch (Exception e) {
            System.err.println("Ошибка сохранения клана " + clan.getName() + ": " + e.getMessage());
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



    public void removeClan(Clan clan) {
        saveLock.lock();
        try {
            File file = new File(directory, clan.getName() + ".clan");
            if (file.exists()) Files.delete(file.toPath());
            clans.remove(clan.getName());
            clan.getMembers().forEach(playerToClan::remove);
        } catch (Exception e) {
            System.err.println("Ошибка удаления клана " + clan.getName() + ": " + e.getMessage());
        } finally {
            saveLock.unlock();
        }
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
        } catch (Exception e) {
            System.err.println("Не удалось прочитать файл клана: " + file.getName());
            return null;
        }
    }

    public File getDirectory() {
        return directory;
    }

    public Map<String, Clan> getClans() {
        return new HashMap<>(clans);
    }
}
