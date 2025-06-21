package net.sxclans.common.clan.menus.management.editor;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.sxclans.bukkit.files.FilesManagerInterface;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.sxclans.bukkit.Depend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClanShopEditorMenu {

    private final ItemsConfig config;
    private final FileConfiguration fileConfig;

    public ClanShopEditorMenu(FilesManagerInterface filesManager) {
        fileConfig = filesManager.getMenuConfig("clan_shop");
        this.config = new ItemsConfig(fileConfig);
    }

    public void openItemsMenu(Player player) {
        AnimatedMenu menu = Plugin.getMenuManager().createMenuFromConfig(
                Plugin.getWithColor().hexToMinecraftColor(config.title()),
                config.rows(),
                player
        );

        for (int slot : config.filler().slots()) {
            if (slot >= 0 && slot < config.rows() * 9) {
                menu.setSlot(slot, new Button(config.filler().material())
                        .setDisplay(" ")
                        .disableInteract(true));
            }
        }

        Button saveButton = new Button(config.saveButton().material());
        saveButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.saveButton().displayName()));
        saveButton.withListener(event -> {
            try {
                saveItemsToConfig(menu);
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                        config.messages().saved()));
            } catch (IOException e) {
                player.sendMessage("&cНеудалось сохранить предметы");
            }
        });
        menu.setSlot(config.saveButton().slot(), saveButton);

        menu.disableInteract(false);

        loadItemsFromConfig(menu);
        menu.open(player);
    }

    private void saveItemsToConfig(AnimatedMenu menu) throws IOException {
        fileConfig.set("items", null);

        ItemStack[] storageContents = menu.getInventory().getStorageContents();

        for (int slotIndex = 0; slotIndex < storageContents.length; slotIndex++) {
            ItemStack item = storageContents[slotIndex];

            if (item != null && !isFillerItem(item) && !isSaveButton(item)) {
                String itemKey = "items.item" + slotIndex;

                fileConfig.set(itemKey + ".price", config.defaultPrice());
                fileConfig.set(itemKey + ".amount", config.defaultAmount());
                fileConfig.set(itemKey + ".name", item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ?
                        Plugin.getWithColor().hexToMinecraftColor(item.getItemMeta().getDisplayName()) : "");
                fileConfig.set(itemKey + ".stack", item);
                fileConfig.set(itemKey + ".required_level", config.defaultRequiredLevel());
                fileConfig.set(itemKey + ".position", slotIndex);

            }
        }

        Depend.getInstance().getFilesManager().saveMenuConfigs();
    }

    private boolean isFillerItem(ItemStack item) {
        return item != null && item.getType() == config.filler().material();
    }

    private boolean isSaveButton(ItemStack item) {
        return item != null && item.getType() == config.saveButton().material() &&
                item.getItemMeta() != null && item.getItemMeta().hasDisplayName() &&
                Plugin.getWithColor().hexToMinecraftColor(item.getItemMeta().getDisplayName())
                        .equals(Plugin.getWithColor().hexToMinecraftColor(config.saveButton().displayName()));
    }

    private void loadItemsFromConfig(AnimatedMenu menu) {
        Optional.ofNullable(fileConfig.getConfigurationSection("items"))
                .ifPresent(itemsSection -> {
                    itemsSection.getKeys(false).forEach(element -> {
                        ItemStack item = fileConfig.getItemStack("items." + element + ".stack");
                        int position = fileConfig.getInt("items." + element + ".position", 0);
                        double price = fileConfig.getDouble("items." + element + ".price", config.defaultPrice());
                        int requiredLevel = fileConfig.getInt("items." + element + ".required_level", config.defaultRequiredLevel());

                        if (item == null) {
                            item = new ItemStack(Material.STONE);
                        }

                        ItemMeta meta = item.getItemMeta();
                        if (meta == null) {
                            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
                        }

                        List<String> lore = new ArrayList<>(meta.getLore());
                        boolean hasDefaultLore = false;
                        List<String> defaultLoreWithPlaceholders = new ArrayList<>();
                        for (String loreLine : config.defaultLore()) {
                            String formattedLine = loreLine.replace("%price%", String.valueOf(price))
                                    .replace("%level%", String.valueOf(requiredLevel));
                            defaultLoreWithPlaceholders.add(Plugin.getWithColor().hexToMinecraftColor(formattedLine));
                            if (lore.contains(formattedLine)) {
                                hasDefaultLore = true;
                            }
                        }

                        if (!hasDefaultLore) {
                            lore.addAll(defaultLoreWithPlaceholders);
                        }

                        meta.setLore(lore);
                        item.setItemMeta(meta);

                        Button menuSlot = new Button(item);
                        menuSlot.setDisplay(Plugin.getWithColor().hexToMinecraftColor(
                                fileConfig.getString("items." + element + ".name", "")
                        ));
                        menu.setSlot(position, menuSlot);
                    });
                });
    }

    private record Filler(Material material, List<Integer> slots) {}

    private record SaveButton(String displayName, Material material, int slot) {}

    private record Messages(String saved) {}

    private static class ItemsConfig {
        private final String title;
        private final int rows;
        private final double defaultPrice;
        private final int defaultAmount;
        private final int defaultRequiredLevel;
        private final List<String> defaultLore;
        private final Filler filler;
        private final SaveButton saveButton;
        private final Messages messages;

        public ItemsConfig(FileConfiguration config) {
            this.title = config.getString("editor_menu.title", "&aРедактор предметов");
            this.rows = config.getInt("editor_menu.rows", 6);
            this.defaultPrice = config.getDouble("editor_menu.default_price", 1.0);
            this.defaultAmount = config.getInt("editor_menu.default_amount", 1);
            this.defaultRequiredLevel = config.getInt("editor_menu.default_required_level", 1);
            this.defaultLore = config.getStringList("editor_menu.default_lore");

            String fillerMaterial = config.getString("editor_menu.filler.material", "GRAY_STAINED_GLASS_PANE");
            List<Integer> fillerSlots = config.getIntegerList("editor_menu.filler.slots");
            this.filler = new Filler(Material.valueOf(fillerMaterial.toUpperCase()), fillerSlots != null ? fillerSlots : new ArrayList<>());

            String saveButtonName = config.getString("editor_menu.save_button.display_name", "&aСохранить предметы");
            String saveButtonMaterial = config.getString("editor_menu.save_button.material", "GREEN_STAINED_GLASS_PANE");
            int saveButtonSlot = config.getInt("editor_menu.save_button.slot", 53);
            this.saveButton = new SaveButton(saveButtonName, Material.valueOf(saveButtonMaterial.toUpperCase()), saveButtonSlot);

            String savedMessage = config.getString("editor_menu.messages.saved", "&aИзменения успешно сохранены!");
            this.messages = new Messages(savedMessage);
        }

        public String title() { return title; }
        public int rows() { return rows; }
        public double defaultPrice() { return defaultPrice; }
        public int defaultAmount() { return defaultAmount; }
        public int defaultRequiredLevel() { return defaultRequiredLevel; }
        public List<String> defaultLore() { return defaultLore != null ? defaultLore : new ArrayList<>(); }
        public Filler filler() { return filler; }
        public SaveButton saveButton() { return saveButton; }
        public Messages messages() { return messages; }
    }
}