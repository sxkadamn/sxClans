package net.sxclans.common.clan.war.arena.serilization;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import net.sxclans.bukkit.Depend;
import net.sxclans.common.Utility;
import net.sxclans.common.clan.war.arena.Arena;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

public class CompileFiles {

    public static void loadFromConfig() {
        File folder = new File("plugins/" + Depend.getInstance().getName() + "/arenas/");
        if (!folder.exists() && !folder.mkdirs()) {
            System.out.println("[ERROR] Failed to create directories: " + folder.getPath());
            return;
        }

        try (Stream<Path> paths = Files.walk(folder.toPath(), 1)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yml"))
                    .forEach(path -> loadArenaFromFile(path.toFile()));
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to read arena files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadArenaFromFile(File file) {
        System.out.println("[INFO] Loading arena from file: " + file.getName());
        String arenaName = file.getName().replaceFirst("[.][^.]+$", "");
        Arena arena = Arena.add(arenaName);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        Location pos1 = Utility.deserializeLocation(yaml, "pos1");
        if (pos1 != null) arena.setClan1Spawns(pos1);

        Location pos2 = Utility.deserializeLocation(yaml, "pos2");
        if (pos2 != null) arena.setClan2Spawns(pos2);

        arena.launch();
    }

    public static void writeToConfig() {
        Arena.getArenas().forEach(CompileFiles::writeArenaToFile);
    }

    private static void writeArenaToFile(Arena arena) {
        File file = new File("plugins/" + Depend.getInstance().getName() + "/arenas/" + arena.getName() + ".yml");
        File parentDir = file.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            System.out.println("[ERROR] Failed to create directories: " + parentDir.getPath());
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        Utility.serializeLocation(config, "pos1", arena.getClan1Spawns());
        Utility.serializeLocation(config, "pos2", arena.getClan2Spawns());

        try {
            config.save(file);
            System.out.println("[INFO] Successfully saved arena: " + arena.getName());
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to save arena file: " + file.getPath());
            e.printStackTrace();
        }
    }
}
