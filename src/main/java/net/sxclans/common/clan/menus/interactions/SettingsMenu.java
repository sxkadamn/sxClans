package net.sxclans.common.clan.menus.interactions;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.integration.VaultEconomyProvider;
import net.sxclans.common.clan.menus.management.editor.ClanShopEditorMenu;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SettingsMenu {
    private final Player player;
    private final Clan clan;
    private final FilesManagerInterface filesManager;
    private final AnimatedMenu menu;
    private final SettingsConfig config;

    public SettingsMenu(Player player, Clan clan, FilesManagerInterface filesManager) {
        this.player = player;
        this.clan = clan;
        this.filesManager = filesManager;

        FileConfiguration fileConfig = filesManager.getMenuConfig("clan_settings");
        this.config = new SettingsConfig(
                fileConfig.getString("title"),
                fileConfig.getInt("size"),
                fileConfig.getString("messages.no_permission"),
                fileConfig.getInt("settings.member_limit_increase_step"),
                fileConfig.getDouble("settings.member_limit_increase_cost"),
                fileConfig.getInt("settings.max_member_limit"),
                fileConfig.getString("messages.max_limit_reached"),
                fileConfig.getString("messages.pvp_enabled"),
                fileConfig.getString("messages.pvp_disabled"),
                fileConfig.getString("messages.base_set"),
                fileConfig.getString("messages.insufficient_funds"),
                fileConfig.getString("messages.limit_increased"),
                fileConfig.getString("messages.shop_editor_open"),
                Stream.of("pvp_toggle", "set_base", "increase_limit", "shop_editor")
                        .collect(Collectors.toMap(
                                key -> key,
                                key -> new ButtonConfig(
                                        key.equals("pvp_toggle") ? fileConfig.getString("buttons." + key + ".enabled.material") : null,
                                        key.equals("pvp_toggle") ? fileConfig.getString("buttons." + key + ".disabled.material") : null,
                                        key.equals("pvp_toggle") ? fileConfig.getInt("buttons." + key + ".enabled.slot") : fileConfig.getInt("buttons." + key + ".slot"),
                                        key.equals("pvp_toggle") ? fileConfig.getString("buttons." + key + ".enabled.display") : null,
                                        key.equals("pvp_toggle") ? fileConfig.getString("buttons." + key + ".disabled.display") : null,
                                        key.equals("pvp_toggle") ? fileConfig.getString("buttons." + key + ".enabled.lore") : null,
                                        key.equals("pvp_toggle") ? fileConfig.getString("buttons." + key + ".disabled.lore") : null,
                                        fileConfig.getString("messages." + (key.equals("pvp_toggle") ? "pvp_enabled" :
                                                key.equals("set_base") ? "base_set" :
                                                        key.equals("shop_editor") ? "shop_editor_open" : "limit_increased")),
                                        key.equals("increase_limit") ? fileConfig.getString("messages.insufficient_funds") : null,
                                        key.equals("pvp_toggle") ? null : fileConfig.getString("buttons." + key + ".material"),
                                        null,
                                        key.equals("pvp_toggle") ? null : fileConfig.getString("buttons." + key + ".display"),
                                        fileConfig.getStringList("buttons." + key + ".lore")
                                ))),
                Material.valueOf(fileConfig.getString("filler.material", "GRAY_STAINED_GLASS_PANE")),
                fileConfig.getIntegerList("filler.slots")
        );
        this.menu = Optional.ofNullable(config)
                .map(cfg -> Plugin.getMenuManager().createMenuFromConfig(
                        Plugin.getWithColor().hexToMinecraftColor(cfg.title()),
                        cfg.size(),
                        player))
                .orElse(null);

        if (!clan.hasPermission(player.getName(), ClanRank.MODERATOR)) {
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.noPermission()));
        }
    }

    public void open() {
        if (menu == null || config == null) return;

        for (int slot : config.fillerSlots()) {
            if (slot >= 0 && slot < config.size() * 9) {
                menu.setSlot(slot, new Button(config.fillerMaterial())
                        .setDisplay(" ")
                        .disableInteract(true)
                        .applyMeta(meta -> meta));
            }
        }

        boolean pvpEnabled = clan.isPvpEnabled();
        int currentLimit = clan.getMemberLimit();
        int newLimit = currentLimit + config.memberLimitIncreaseStep();
        double cost = config.memberLimitIncreaseCost();
        int maxLimit = config.maxMemberLimit();

        if (newLimit > maxLimit) {
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.maxLimitReached()));
            return;
        }

        Stream.of("pvp_toggle", "set_base", "increase_limit", "shop_editor")
                .forEach(type -> {
                    ButtonConfig btnConfig = config.buttons().get(type);
                    Material material = type.equals("pvp_toggle")
                            ? Material.valueOf(pvpEnabled ? btnConfig.disabledMaterial() : btnConfig.enabledMaterial())
                            : Material.valueOf(btnConfig.material());
                    String display = Plugin.getWithColor().hexToMinecraftColor(type.equals("pvp_toggle")
                            ? (pvpEnabled ? btnConfig.disabledDisplay() : btnConfig.enabledDisplay())
                            : btnConfig.display());
                    List<String> lore = type.equals("pvp_toggle")
                            ? List.of(Plugin.getWithColor().hexToMinecraftColor(pvpEnabled ? btnConfig.disabledLore() : btnConfig.enabledLore()))
                            : btnConfig.lore().stream()
                            .map(line -> Plugin.getWithColor().hexToMinecraftColor(
                                    line.replace("%current_limit%", String.valueOf(currentLimit))
                                            .replace("%new_limit%", String.valueOf(newLimit))
                                            .replace("%cost%", String.valueOf(cost))))
                            .collect(Collectors.toList());

                    Button button = new Button(material)
                            .setDisplay(display)
                            .setLoreList(lore)
                            .withListener(event -> {
                                switch (type) {
                                    case "pvp_toggle" -> {
                                        clan.setPvpEnabled(!pvpEnabled);
                                        clan.save();
                                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(pvpEnabled ? config.pvpDisabledMessage() : config.pvpEnabledMessage()));
                                    }
                                    case "set_base" -> {
                                        var location = player.getLocation();
                                        clan.setBaseLocation(location);
                                        clan.save();
                                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                                config.baseSetMessage()
                                                        .replace("%x%", String.valueOf(location.getBlockX()))
                                                        .replace("%y%", String.valueOf(location.getBlockY()))
                                                        .replace("%z%", String.valueOf(location.getBlockZ()))));
                                    }
                                    case "increase_limit" -> {
                                        VaultEconomyProvider economy = new VaultEconomyProvider();
                                        if (!economy.isAvailable() || economy.getBalance(player) < cost) {
                                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.insufficientFundsMessage()));
                                            return;
                                        }
                                        economy.withdraw(player, cost);
                                        clan.setMemberLimit(newLimit);
                                        clan.save();
                                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                                config.limitIncreasedMessage().replace("%new_limit%", String.valueOf(newLimit))));
                                    }
                                    case "shop_editor" -> {
                                        if (!clan.hasPermission(player.getName(), ClanRank.LEADER)) {
                                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.noPermission()));
                                            return;
                                        }
                                        new ClanShopEditorMenu(filesManager).openItemsMenu(player);
                                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.shopEditorOpenMessage()));
                                    }
                                }
                            });
                    menu.setSlot(btnConfig.slot(), button);
                    menu.refreshItems();
                });

        menu.open(player);
    }

    private record SettingsConfig(
            String title,
            int size,
            String noPermission,
            int memberLimitIncreaseStep,
            double memberLimitIncreaseCost,
            int maxMemberLimit,
            String maxLimitReached,
            String pvpEnabledMessage,
            String pvpDisabledMessage,
            String baseSetMessage,
            String insufficientFundsMessage,
            String limitIncreasedMessage,
            String shopEditorOpenMessage,
            Map<String, ButtonConfig> buttons,
            Material fillerMaterial,
            List<Integer> fillerSlots
    ) {
        public SettingsConfig {
            if (title == null || noPermission == null || maxLimitReached == null || pvpEnabledMessage == null ||
                    pvpDisabledMessage == null || baseSetMessage == null || insufficientFundsMessage == null ||
                    limitIncreasedMessage == null || shopEditorOpenMessage == null || fillerMaterial == null || fillerSlots == null) {
                throw new IllegalArgumentException("Required configuration values are missing in clan_settings.yml");
            }
        }
    }

    private record ButtonConfig(
            String enabledMaterial,
            String disabledMaterial,
            int slot,
            String enabledDisplay,
            String disabledDisplay,
            String enabledLore,
            String disabledLore,
            String successMessage,
            String errorMessage,
            String material,
            Integer slotOverride,
            String display,
            List<String> lore
    ) {
        public ButtonConfig {
            if (slot == 0 && (slotOverride == null || slotOverride == 0)) {
                throw new IllegalArgumentException("Slot is required for button configuration");
            }
        }

        public int slot() {
            return slotOverride != null ? slotOverride : slot;
        }
    }
}