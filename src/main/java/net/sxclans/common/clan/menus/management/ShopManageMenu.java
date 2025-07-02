package net.sxclans.common.clan.menus.management;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.menus.management.api.ClanShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class ShopManageMenu implements ClanShopItem {

    private final Player player;
    private final Clan clan;
    private final AnimatedMenu menu;
    private final FileConfiguration fileConfig;
    private final ClanShopConfig config;

    public ShopManageMenu(Player player, Clan clan, FilesManagerInterface filesManager) {
        this.player = player;
        this.clan = clan;
        this.fileConfig = filesManager.getMenuConfig("clan_shop");
        this.config = new ClanShopConfig(
                fileConfig.getString("menu.title", "&6Магазин клана"),
                fileConfig.getInt("menu.size", 6),
                fileConfig.getString("messages.insufficient_funds", "&cНедостаточно средств в банке клана!"),
                fileConfig.getString("messages.purchase_success", "&aПокупка успешно завершена!"),
                Material.valueOf(fileConfig.getString("menu.filler.material", "GRAY_STAINED_GLASS_PANE").toUpperCase()),
                fileConfig.getIntegerList("menu.filler.slots"),
                fileConfig.getStringList("editor_menu.default_lore")
        );
        this.menu = Plugin.getMenuManager().createMenuFromConfig(
                Plugin.getWithColor().hexToMinecraftColor(config.title()),
                config.size(), player
        );
    }

    public void open() {
        if (menu == null) return;

        config.fillerSlots().forEach(slot -> {
            if (slot >= 0 && slot < config.size() * 9) {
                menu.setSlot(slot, new Button(config.fillerMaterial()).setDisplay(" ").disableInteract(true));
            }
        });

        Optional.ofNullable(fileConfig.getConfigurationSection("items")).ifPresent(section ->
                section.getKeys(false).forEach(key -> {
                    String path = "items." + key;
                    int index = fileConfig.getInt(path + ".position", -1);
                    if (index < 0) return;

                    double price = fileConfig.getDouble(path + ".price", 1.0);
                    int amount = fileConfig.getInt(path + ".amount", 1);
                    int requiredLevel = fileConfig.getInt(path + ".required_level", 1);
                    String name = fileConfig.getString(path + ".name", "");

                    ItemStack item = Optional.ofNullable(fileConfig.getItemStack(path + ".stack")).orElse(new ItemStack(Material.STONE));
                    item.setAmount(amount);
                    ItemMeta meta = Optional.ofNullable(item.getItemMeta()).orElse(item.getItemMeta());
                    List<String> lore = new ArrayList<>(Optional.ofNullable(meta.getLore()).orElse(new ArrayList<>()));

                    config.defaultLore().stream()
                            .map(line -> Plugin.getWithColor().hexToMinecraftColor(line
                                    .replace("%price%", String.valueOf(price))
                                    .replace("%level%", String.valueOf(requiredLevel))))
                            .filter(line -> !lore.contains(line))
                            .forEach(lore::add);

                    if (!name.isEmpty()) meta.setDisplayName(Plugin.getWithColor().hexToMinecraftColor(name));
                    meta.setLore(lore);
                    item.setItemMeta(meta);

                    if (clan.getLevel() < requiredLevel) {
                        ItemStack locked = new ItemStack(Material.BARRIER);
                        ItemMeta lockedMeta = locked.getItemMeta();
                        lockedMeta.setDisplayName(Plugin.getWithColor().hexToMinecraftColor("&cНедоступно"));
                        lockedMeta.setLore(List.of(
                                Plugin.getWithColor().hexToMinecraftColor("&7Требуемый уровень: &c" + requiredLevel),
                                Plugin.getWithColor().hexToMinecraftColor("&7Ваш уровень: &c" + clan.getLevel())
                        ));
                        locked.setItemMeta(lockedMeta);
                        menu.setSlot(index, new Button(locked).disableInteract(true));
                    } else {
                        menu.setSlot(index, new Button(item).withListener(e -> {
                            if (clan.getBank() < price) {
                                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.insufficientFunds()));
                                return;
                            }
                            clan.withdrawMoneyFromBank(price);
                            clan.save();
                            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                            leftover.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.purchaseSuccess()));
                        }));
                    }
                })
        );

        menu.open(player);
    }

    @Override
    public boolean canBeViewedBy(Clan clan, int requiredLevel) {
        return clan.getLevel() >= requiredLevel;
    }

    public record ClanShopConfig(
            String title,
            int size,
            String insufficientFunds,
            String purchaseSuccess,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            List<String> defaultLore
    ) {}
}
