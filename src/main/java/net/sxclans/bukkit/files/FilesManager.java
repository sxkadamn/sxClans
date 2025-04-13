package net.sxclans.bukkit.files;


import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FilesManager implements FilesManagerInterface {
    private final File menusFolder;
    private final Map<String, FileConfiguration> menuConfigs;
    private final JavaPlugin plugin;

    public FilesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.menusFolder = new File(plugin.getDataFolder(), "menus");
        this.menuConfigs = new HashMap<>();

        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
        }

        copyDefaultMenuConfigs();

        loadMenuConfigs();
    }

    private void copyDefaultMenuConfigs() {
        List<String> menuFiles = getResourceFiles("menus");

        for (String menuFile : menuFiles) {
            File file = new File(menusFolder, menuFile);
            if (!file.exists()) {
                try (InputStream in = plugin.getResource("menus/" + menuFile)) {
                    if (in == null) {
                        continue;
                    }
                    Files.copy(in, file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<String> getResourceFiles(String path) {
        List<String> filenames = new ArrayList<>();

        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jarFile.isFile()) {
                try (JarFile jar = new JarFile(jarFile)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(path + "/") && name.endsWith(".yml")) {
                            String fileName = name.substring((path + "/").length());
                            filenames.add(fileName);
                        }
                    }
                }
            } else {
                File dir = new File(plugin.getClass().getClassLoader().getResource(path).toURI());
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
                    if (files != null) {
                        for (File file : files) {
                            filenames.add(file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return filenames;
    }

    public void loadMenuConfigs() {
        Optional.ofNullable(menusFolder.listFiles((dir, name) -> name.endsWith(".yml")))
                .stream()
                .flatMap(Arrays::stream)
                .forEach(this::loadMenuConfigFromFile);
    }

    private void loadMenuConfigFromFile(File file) {
        String menuKey = file.getName().replace(".yml", "");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        menuConfigs.put(menuKey, config);
    }

    public void saveMenuConfigs() {
        menuConfigs.forEach((menuKey, config) -> saveMenuConfigToFile(new File(menusFolder, menuKey + ".yml"), config));
    }

    public void saveConfig(String configName, FileConfiguration config) {
        if (configName == null || config == null) {
            throw new IllegalArgumentException("Config name and configuration cannot be null");
        }

        File file = new File(menusFolder, configName + ".yml");
        saveMenuConfigToFile(file, config);
    }


    private void saveMenuConfigToFile(File file, FileConfiguration config) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            if (!file.exists()) {
                file.createNewFile();
            }
            config.save(file);

            String menuKey = file.getName().replace(".yml", "");
            menuConfigs.put(menuKey, YamlConfiguration.loadConfiguration(file));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void removeMenuConfig(String menuKey) {
        File file = new File(menusFolder, menuKey + ".yml");
        if (file.exists()) {
            file.delete();
            menuConfigs.remove(menuKey);
        }
    }

    public FileConfiguration getMenuConfig(String menuKey) {
        return menuConfigs.get(menuKey);
    }

    public void reloadMenuConfigs() {
        menuConfigs.clear();
        loadMenuConfigs();
    }

    public List<FileConfiguration> getAllMenuConfigs() {
        return new ArrayList<>(menuConfigs.values());
    }

    public File getMenusFolder() {
        return menusFolder;
    }

    public Map<String, FileConfiguration> getMenuConfigs() {
        return Collections.unmodifiableMap(menuConfigs);
    }
}
