package net.sxclans.bukkit.files;

import org.bukkit.configuration.file.FileConfiguration;

public interface FilesManagerInterface {

    FileConfiguration getMenuConfig(String menuKey);

    void reloadMenuConfigs();

    void loadMenuConfigs();
}