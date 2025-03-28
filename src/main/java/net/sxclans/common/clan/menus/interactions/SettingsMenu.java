package net.sxclans.common.clan.menus.interactions;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.lielibrary.gui.buttons.ButtonListener;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.integration.VaultEconomyProvider;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class SettingsMenu {

    private final Player player;
    private final Clan clan;
    private final FilesManagerInterface filesManager;
    private AnimatedMenu menu;

    public SettingsMenu(Player player, Clan clan, FilesManagerInterface filesManager) {
        this.player = player;
        this.clan = clan;
        this.filesManager = filesManager;

        FileConfiguration config = filesManager.getMenuConfig("clan_settings");
        String title = Plugin.getWithColor().hexToMinecraftColor(config.getString("title", "&6Настройки клана"));
        int size = config.getInt("size");

        if (!clan.hasPermission(player.getName(), ClanRank.MODERATOR)) {
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("messages.no_permission", "&cУ вас нет прав для управления настройками клана!")));
            return;
        }

        this.menu = Plugin.getMenuManager().createMenuFromConfig(title, size, player);
    }

    public void open() {
        if (menu == null) return;

        FileConfiguration config = filesManager.getMenuConfig("clan_settings");

        boolean pvpEnabled = clan.isPvpEnabled();
        int currentLimit = clan.getMemberLimit();
        int memberLimitIncreaseStep = config.getInt("settings.member_limit_increase_step", 5);
        int newLimit = currentLimit + memberLimitIncreaseStep;
        double cost = config.getDouble("settings.member_limit_increase_cost", 1000.0);
        int maxLimit = config.getInt("settings.max_member_limit", 50);

        if (newLimit > maxLimit) {
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("messages.max_limit_reached", "&cДостигнут максимальный лимит участников!")));
            return;
        }

        // Кнопка переключения PvP
        Material pvpEnabledMaterial = Material.DIAMOND_SWORD;
        try {
            pvpEnabledMaterial = Material.valueOf(config.getString("buttons.pvp_toggle.enabled.material", "DIAMOND_SWORD").toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        Material pvpDisabledMaterial = Material.IRON_SWORD;
        try {
            pvpDisabledMaterial = Material.valueOf(config.getString("buttons.pvp_toggle.disabled.material", "IRON_SWORD").toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        Button pvpButton = new Button(pvpEnabled ? pvpDisabledMaterial : pvpEnabledMaterial);
        pvpButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(
                pvpEnabled
                        ? config.getString("buttons.pvp_toggle.disabled.display", "&cPvP: Выключено")
                        : config.getString("buttons.pvp_toggle.enabled.display", "&aPvP: Включено")
        ));
        pvpButton.setLore(Plugin.getWithColor().hexToMinecraftColor(
                pvpEnabled
                        ? config.getString("buttons.pvp_toggle.disabled.lore", "&7Нажмите, чтобы включить PvP")
                        : config.getString("buttons.pvp_toggle.enabled.lore", "&7Нажмите, чтобы выключить PvP")
        ));
        pvpButton.withListener(event -> {
            clan.setPvpEnabled(!pvpEnabled);
            clan.save();
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                    pvpEnabled
                            ? config.getString("messages.pvp_disabled", "&cPvP выключено!")
                            : config.getString("messages.pvp_enabled", "&aPvP включено!")
            ));
            player.closeInventory();
            new SettingsMenu(player, clan, filesManager).open();
        });
        menu.setSlot(config.getInt("buttons.pvp_toggle.enabled.slot", 10), pvpButton);

        // Кнопка установки базы
        Material setBaseMaterial = Material.ENDER_PEARL;
        try {
            setBaseMaterial = Material.valueOf(config.getString("buttons.set_base.material", "ENDER_PEARL").toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        Button setBaseButton = new Button(setBaseMaterial);
        setBaseButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("buttons.set_base.display", "&bУстановить базу")));
        setBaseButton.setLore(Plugin.getWithColor().hexToMinecraftColor(config.getString("buttons.set_base.lore", "&7Установите точку базы клана на вашей текущей позиции")));
        setBaseButton.withListener(event -> {
            Location location = player.getLocation();
            clan.setBaseLocation(location);
            clan.save();
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                    config.getString("messages.base_set", "&aБаза установлена на координаты: %x%, %y%, %z%!")
                            .replace("%x%", String.valueOf(location.getBlockX()))
                            .replace("%y%", String.valueOf(location.getBlockY()))
                            .replace("%z%", String.valueOf(location.getBlockZ()))
            ));
            player.closeInventory();
        });
        menu.setSlot(config.getInt("buttons.set_base.slot", 12), setBaseButton);

        Material increaseLimitMaterial = Material.EMERALD;
        try {
            increaseLimitMaterial = Material.valueOf(config.getString("buttons.increase_limit.material", "EMERALD").toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        List<String> increaseLimitLore = config.getStringList("buttons.increase_limit.lore");
        if (increaseLimitLore.isEmpty()) {
            increaseLimitLore = Arrays.asList(
                    "&7Текущий лимит: %current_limit%",
                    "&7Новый лимит: %new_limit%",
                    "&7Стоимость: %cost%"
            );
        }
        increaseLimitLore = increaseLimitLore.stream()
                .map(line -> Plugin.getWithColor().hexToMinecraftColor(
                        line.replace("%current_limit%", String.valueOf(currentLimit))
                                .replace("%cost%", String.valueOf(cost))
                                .replace("%new_limit%", String.valueOf(newLimit))
                ))
                .toList();

        Button increaseLimitButton = new Button(increaseLimitMaterial);
        increaseLimitButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("buttons.increase_limit.display", "&eУвеличить лимит участников")));
        increaseLimitButton.setLoreList(increaseLimitLore);
        increaseLimitButton.withListener(event -> {
            VaultEconomyProvider economy = new VaultEconomyProvider();
            if (!economy.isAvailable() || economy.getBalance(player) < cost) {
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("messages.insufficient_funds", "&cНедостаточно средств для увеличения лимита!")));
                return;
            }

            economy.withdraw(player, cost);
            clan.setMemberLimit(newLimit);
            clan.save();
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                    config.getString("messages.limit_increased", "&aЛимит участников увеличен до %new_limit%!")
                            .replace("%new_limit%", String.valueOf(newLimit))
            ));
            player.closeInventory();
            new SettingsMenu(player, clan, filesManager).open();
        });
        menu.setSlot(config.getInt("buttons.increase_limit.slot", 14), increaseLimitButton);

        menu.open(player);
    }
}
