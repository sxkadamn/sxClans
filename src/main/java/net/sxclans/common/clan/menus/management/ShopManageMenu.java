package net.sxclans.common.clan.menus.management;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.lielibrary.gui.buttons.ButtonListener;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShopManageMenu {
    private final Player player;
    private final Clan clan;
    private final AnimatedMenu menu;
    private final ClanShopConfig config;
    private final FileConfiguration fileConfig;

    public ShopManageMenu(Player player, Clan clan, FilesManagerInterface filesManager) {
        this.player = player;
        this.clan = clan;
        this.fileConfig = filesManager.getMenuConfig("clan_shop");
        this.config = new ClanShopConfig(
                fileConfig.getString("menu.title", "Clan Shop"),
                fileConfig.getInt("menu.size", 6),
                fileConfig.getString("messages.insufficient_funds", "&cInsufficient funds!"),
                fileConfig.getString("messages.purchase_success", "&aPurchase successful!"),
                Material.valueOf(fileConfig.getString("menu.filler.material", "GRAY_STAINED_GLASS_PANE").toUpperCase()),
                fileConfig.getIntegerList("menu.filler.slots"),
                fileConfig.getStringList("editor_menu.default_lore")
        );
        this.menu = Plugin.getMenuManager().createMenuFromConfig(
                Plugin.getWithColor().hexToMinecraftColor(config.title()),
                config.size(),
                player
        );
    }

    public void open() {
        if (menu == null || config == null) return;

        for (int slot : config.fillerSlots()) {
            if (slot >= 0 && slot < config.size() * 9) {
                menu.setSlot(slot, new Button(config.fillerMaterial())
                        .setDisplay(" ")
                        .disableInteract(true));
            }
        }

        loadItemsFromConfig(menu);

        menu.open(player);
    }

    private Map.Entry<Integer, Button> createButton(int index, Map<String, Object> itemData) {
        double price = Optional.ofNullable(itemData.get("price"))
                .map(value -> ((Number) value).doubleValue())
                .orElse(0.0);

        int amount = Optional.ofNullable(itemData.get("amount"))
                .map(value -> ((Number) value).intValue())
                .orElse(1);

        String name = Optional.ofNullable(itemData.get("name"))
                .map(value -> (String) value)
                .orElse("");

        int requiredLevel = Optional.ofNullable(itemData.get("required_level"))
                .map(value -> ((Number) value).intValue())
                .orElse(0);

        ItemStack itemStack = Optional.ofNullable(fileConfig.getItemStack("items.item" + index + ".stack"))
                .orElseGet(() -> new ItemStack(Material.STONE, amount));
        itemStack.setAmount(amount);

        ItemMeta meta = Optional.ofNullable(itemStack.getItemMeta())
                .orElseThrow(() -> new IllegalStateException("ItemMeta cannot be null"));

        List<String> lore = (List<String>) Stream.of(
                        meta.hasLore() ? meta.getLore() : Collections.emptyList(),
                        fileConfig.contains("items.item" + index + ".lore")
                                ? fileConfig.getStringList("items.item" + index + ".lore").stream()
                                .map(Plugin.getWithColor()::hexToMinecraftColor)
                                .toList()
                                : Collections.emptyList()
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<String> defaultLoreWithPlaceholders = new ArrayList<>();
        boolean hasDefaultLore = config.defaultLore().stream()
                .map(line -> line.replace("%price%", String.valueOf(price))
                        .replace("%level%", String.valueOf(requiredLevel)))
                .map(Plugin.getWithColor()::hexToMinecraftColor)
                .peek(defaultLoreWithPlaceholders::add)
                .anyMatch(lore::contains);

        if (!hasDefaultLore) {
            lore.addAll(defaultLoreWithPlaceholders);
        }

        meta.setLore(lore);
        itemStack.setItemMeta(meta);

        if (clan.getLevel() < requiredLevel) return null;

        ButtonConfig buttonConfig = new ButtonConfig(
                index,
                itemStack.getType(),
                name,
                lore,
                itemStack
        );

        return Map.entry(index, buttonConfig.toButton(event -> {
            if (clan.getBank() < price) {
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.insufficientFunds()));
                return;
            }

            clan.withdrawMoneyFromBank(price);
            clan.save();

            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(itemStack);
            leftOver.values().forEach(leftOverItem -> player.getWorld().dropItem(player.getLocation(), leftOverItem));

            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.purchaseSuccess()));
        }));
    }

    private void loadItemsFromConfig(AnimatedMenu menu) {
        Optional.ofNullable(fileConfig.getConfigurationSection("items"))
                .ifPresent(itemsSection -> {
                    itemsSection.getKeys(false).forEach(itemKey -> {
                        int index;
                        try {
                            index = Integer.parseInt(itemKey.replace("item", ""));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            return;
                        }

                        Map<String, Object> itemData = fileConfig.getConfigurationSection("items." + itemKey)
                                .getValues(false);

                        Map.Entry<Integer, Button> buttonEntry = createButton(index, itemData);

                        if (buttonEntry != null) {
                            menu.setSlot(buttonEntry.getKey(), buttonEntry.getValue());
                        }
                    });
                });
    }

    private record ButtonConfig(
            int position,
            Material material,
            String display,
            List<String> lore,
            ItemStack item
    ) {
        public Button toButton(ButtonListener listener) {
            Button button = item != null ?
                    new Button(item) :
                    new Button(material)
                            .setDisplay(Plugin.getWithColor().hexToMinecraftColor(display))
                            .setLoreList(lore != null ?
                                    lore.stream()
                                            .map(line -> Plugin.getWithColor().hexToMinecraftColor(line))
                                            .toList()
                                    : null);

            button.withListener(listener);
            return button;
        }
    }

    private record ClanShopConfig(
            String title,
            int size,
            String insufficientFunds,
            String purchaseSuccess,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            List<String> defaultLore
    ) {
        public ClanShopConfig {
            Objects.requireNonNull(title, "Title cannot be null");
            Objects.requireNonNull(insufficientFunds, "Insufficient funds message cannot be null");
            Objects.requireNonNull(purchaseSuccess, "Purchase success message cannot be null");
            Objects.requireNonNull(fillerMaterial, "Filler material cannot be null");
            Objects.requireNonNull(fillerSlots, "Filler slots cannot be null");
            Objects.requireNonNull(defaultLore, "Default lore cannot be null");
            if (size <= 0) throw new IllegalArgumentException("Size must be positive");
        }

        public List<String> defaultLore() { return defaultLore; }
        public String title() { return title; }
        public int size() { return size; }
        public String insufficientFunds() { return insufficientFunds; }
        public String purchaseSuccess() { return purchaseSuccess; }
        public Material fillerMaterial() { return fillerMaterial; }
        public List<Integer> fillerSlots() { return fillerSlots; }
    }
}