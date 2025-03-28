package net.sxclans.bukkit.files;

import org.bukkit.configuration.file.FileConfiguration;

public interface FilesManagerInterface {
    /**
     * Получает конфигурацию меню по ключу.
     *
     * @param menuKey Ключ меню (например, "deposit" для deposit.yml).
     * @return FileConfiguration для указанного меню, или null, если файл не найден.
     */
    FileConfiguration getMenuConfig(String menuKey);

    /**
     * Перезагружает все конфигурации меню из папки.
     */
    void reloadMenuConfigs();

    void loadMenuConfigs();
}