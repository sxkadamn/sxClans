package net.sxclans.common.clan.menus.management.api;

import org.bukkit.entity.Player;

public interface ItemEditorHandler {
    void editPrice(Player player, int slot);
    void editLevel(Player player, int slot);
}