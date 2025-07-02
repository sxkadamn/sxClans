package net.sxclans.common.clan.menus.management.editor;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.lielibrary.gui.buttons.ButtonListener;
import net.sxclans.bukkit.Depend;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.menus.management.api.ItemEditorHandler;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.*;

public class ClanShopEditorMenu {

    private final FileConfiguration config;
    private final int rows;
    private final String title;
    private final Material fillerMaterial;
    private final List<Integer> fillerSlots;
    private final Material saveMaterial;
    private final String saveDisplay;
    private final int saveSlot;
    private final List<String> defaultLore;
    private final double defaultPrice;
    private final int defaultAmount;
    private final int defaultLevel;
    private final String messageSaved;

    public ClanShopEditorMenu(FilesManagerInterface filesManager) {
        this.config = filesManager.getMenuConfig("clan_shop");

        this.rows = config.getInt("editor_menu.rows", 6);
        this.title = Plugin.getWithColor().hexToMinecraftColor(config.getString("editor_menu.title", "&aРедактор предметов"));
        this.fillerMaterial = Material.valueOf(config.getString("editor_menu.filler.material", "GRAY_STAINED_GLASS_PANE").toUpperCase());
        this.fillerSlots = config.getIntegerList("editor_menu.filler.slots");
        this.saveMaterial = Material.valueOf(config.getString("editor_menu.save_button.material", "EMERALD_BLOCK").toUpperCase());
        this.saveDisplay = Plugin.getWithColor().hexToMinecraftColor(config.getString("editor_menu.save_button.display_name", "&aСохранить"));
        this.saveSlot = config.getInt("editor_menu.save_button.slot", 49);
        this.defaultLore = config.getStringList("editor_menu.default_lore");
        this.defaultPrice = config.getDouble("editor_menu.default_price", 1.0);
        this.defaultAmount = config.getInt("editor_menu.default_amount", 1);
        this.defaultLevel = config.getInt("editor_menu.default_required_level", 1);
        this.messageSaved = Plugin.getWithColor().hexToMinecraftColor(config.getString("editor_menu.messages.saved", "&aСохранено!"));
    }

    public void open(Player player) {
        AnimatedMenu menu = Plugin.getMenuManager().createMenuFromConfig(title, rows, player);

        for (int slot : fillerSlots) {
            if (slot >= 0 && slot < rows * 9) {
                menu.setSlot(slot, new Button(fillerMaterial).setDisplay(" ").disableInteract(true));
            }
        }

        loadItems(menu);

        Button saveBtn = new Button(saveMaterial)
                .setDisplay(saveDisplay)
                .withListener(event -> {
                    try {
                        saveItems(menu);
                        player.sendMessage(messageSaved);
                    } catch (IOException e) {
                        player.sendMessage("&cОшибка при сохранении предметов: " + e.getMessage());
                    }
                });

        menu.setSlot(saveSlot, saveBtn);
        menu.open(player);
    }

    private void loadItems(AnimatedMenu menu) {
        Optional.ofNullable(config.getConfigurationSection("items")).ifPresent(section -> {
            for (String key : section.getKeys(false)) {
                String path = "items." + key;

                ItemStack item = config.getItemStack(path + ".stack");
                int position = config.getInt(path + ".position", 0);
                double price = config.getDouble(path + ".price", defaultPrice);
                int level = config.getInt(path + ".required_level", defaultLevel);
                String name = config.getString(path + ".name", "");

                if (item == null) item = new ItemStack(Material.STONE);

                ItemMeta meta = item.getItemMeta();
                if (meta == null) return;

                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                List<String> formatted = new ArrayList<>();
                for (String l : defaultLore) {
                    formatted.add(Plugin.getWithColor().hexToMinecraftColor(
                            l.replace("%price%", String.valueOf(price))
                                    .replace("%level%", String.valueOf(level))
                    ));
                }
                if (!lore.containsAll(formatted)) lore.addAll(formatted);

                meta.setLore(lore);
                if (!name.isEmpty()) meta.setDisplayName(Plugin.getWithColor().hexToMinecraftColor(name));
                item.setItemMeta(meta);

                int finalPosition = position;
                menu.setSlot(position, new Button(item)
                        .withListener(event -> {
                            if (event.isRightClick()) {
                                openItemEditMenu((Player) event.getWhoClicked(), finalPosition);
                            }
                        })
                        .setDisplay(meta.getDisplayName()));
            }
        });
    }

    private void saveItems(AnimatedMenu menu) throws IOException {
        config.set("items", null);
        ItemStack[] contents = menu.getInventory().getStorageContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || isFiller(item) || isSave(item)) continue;

            String path = "items.item" + i;

            ItemStack cloned = item.clone();
            ItemMeta meta = cloned.getItemMeta();

            if (meta != null && meta.hasLore()) {
                List<String> stripped = meta.getLore().stream().map(this::stripToMinecraftColor).toList();
                meta.setLore(stripped);
                cloned.setItemMeta(meta);
            }

            config.set(path + ".stack", cloned);
            config.set(path + ".price", defaultPrice);
            config.set(path + ".amount", defaultAmount);
            config.set(path + ".required_level", defaultLevel);
            config.set(path + ".position", i);
            config.set(path + ".name", meta != null && meta.hasDisplayName() ? Plugin.getWithColor().hexToMinecraftColor(meta.getDisplayName()) : "");
        }

        Depend.getInstance().getFilesManager().saveMenuConfigs();
    }

    private void openItemEditMenu(Player player, int slot) {
        AnimatedMenu editMenu = Plugin.getMenuManager().createMenuFromConfig("&6Редактирование предмета", 3, player);
        ItemEditorHandler handler = new DefaultItemEditorHandler(config, () -> open(player));

        editMenu.setSlot(11, new Button(Material.GOLD_INGOT)
                .setDisplay("&6Изменить цену")
                .withListener(e -> handler.editPrice(player, slot)));

        editMenu.setSlot(15, new Button(Material.EXPERIENCE_BOTTLE)
                .setDisplay("&bИзменить уровень")
                .withListener(e -> handler.editLevel(player, slot)));

        editMenu.open(player);
    }



    private boolean isFiller(ItemStack item) {
        return item.getType() == fillerMaterial;
    }

    private boolean isSave(ItemStack item) {
        if (item.getType() != saveMaterial) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && Plugin.getWithColor().hexToMinecraftColor(meta.getDisplayName()).equals(saveDisplay);
    }

    private String stripToMinecraftColor(String input) {
        try {
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                    .legacySection()
                    .serialize(net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(input));
        } catch (Exception e) {
            return input;
        }
    }
}
