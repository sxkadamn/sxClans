package net.sxclans.common.clan.menus.management.editor;

import net.lielibrary.bukkit.Plugin;
import net.sxclans.common.clan.menus.management.api.ItemEditorHandler;
import net.wesjd.anvilgui.AnvilGUI;
import net.sxclans.bukkit.Depend;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class DefaultItemEditorHandler implements ItemEditorHandler {

    private final FileConfiguration config;
    private final Runnable refreshMenu;

    public DefaultItemEditorHandler(FileConfiguration config, Runnable refreshMenu) {
        this.config = config;
        this.refreshMenu = refreshMenu;
    }

    @Override
    public void editPrice(Player player, int slot) {
        player.closeInventory();
        new AnvilGUI.Builder()
                .plugin(Depend.getInstance())
                .title("&6Введите цену")
                .text("0.0")
                .itemLeft(new ItemStack(Material.GOLD_INGOT))
                .onClick((slotClick, stateSnapshot) -> {
                    if (slotClick != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    try {
                        double newPrice = Double.parseDouble(stateSnapshot.getText().trim());
                        config.set("items.item" + slot + ".price", newPrice);
                        Depend.getInstance().getFilesManager().saveMenuConfigs();
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("&aЦена обновлена: " + newPrice));
                        return List.of(AnvilGUI.ResponseAction.close());
                    } catch (NumberFormatException e) {
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("&cНеверное число"));
                    }
                }).open(player);

        Bukkit.getScheduler().runTaskLater(Depend.getInstance(), refreshMenu, 20L);
    }

    @Override
    public void editLevel(Player player, int slot) {
        player.closeInventory();
        new AnvilGUI.Builder()
                .plugin(Depend.getInstance())
                .title("&bВведите уровень")
                .text("1")
                .itemLeft(new ItemStack(Material.EXPERIENCE_BOTTLE))
                .onClick((slotClick, stateSnapshot) -> {
                    if (slotClick != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    try {
                        int level = Integer.parseInt(stateSnapshot.getText().trim());
                        config.set("items.item" + slot + ".required_level", level);
                        Depend.getInstance().getFilesManager().saveMenuConfigs();
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("&aУровень обновлён: " + level));
                        return List.of(AnvilGUI.ResponseAction.close());
                    } catch (NumberFormatException e) {
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("&cНеверное число"));
                    }
                }).open(player);

        Bukkit.getScheduler().runTaskLater(Depend.getInstance(), refreshMenu, 20L);
    }
}
