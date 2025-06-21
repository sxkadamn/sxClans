package net.sxclans.common.clan.menus.interactions;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.bukkit.requirements.RequirementAPI;
import net.lielibrary.bukkit.requirements.RequirementExecute;
import net.lielibrary.gui.buttons.Button;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.integration.VaultEconomyProvider;
import net.sxclans.common.clan.menus.management.editor.ClanShopEditorMenu;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.io.File;
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
    private final FileConfiguration fileConfig;

    public SettingsMenu(Player player, Clan clan, FilesManagerInterface filesManager) {
        this.player = player;
        this.clan = clan;
        this.filesManager = filesManager;

        fileConfig = filesManager.getMenuConfig("clan_settings");
        RequirementAPI.loadConfig(fileConfig);

        this.config = new SettingsConfig(
                fileConfig.getString("title"),
                fileConfig.getInt("size"),
                fileConfig.getString("messages.no_permission"),
                fileConfig.getInt("settings.member_limit_increase_step"),
                fileConfig.getDouble("settings.member_limit_increase_cost"),
                fileConfig.getInt("settings.max_member_limit"),
                fileConfig.getString("messages.max_limit_reached"),
                fileConfig.getStringList("buttons.pvp_toggle.commands"),
                fileConfig.getStringList("buttons.set_base.commands"),
                fileConfig.getStringList("buttons.increase_limit.success_commands"),
                fileConfig.getStringList("buttons.increase_limit.error_commands"),
                fileConfig.getStringList("buttons.shop_editor.commands"),
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
                                        key.equals("pvp_toggle") ? null : fileConfig.getString("buttons." + key + ".material"),
                                        key.equals("pvp_toggle") ? null : fileConfig.getString("buttons." + key + ".display"),
                                        fileConfig.getStringList("buttons." + key + ".lore")
                                ))),
                getMaterial(fileConfig.getString("filler.material", "GRAY_STAINED_GLASS_PANE")),
                fileConfig.getIntegerList("filler.slots").stream().distinct().collect(Collectors.toList())
        );
        this.menu = Optional.of(config)
                .map(cfg -> Plugin.getMenuManager().createMenuFromConfig(
                        Plugin.getWithColor().hexToMinecraftColor(cfg.title()),
                        cfg.size(),
                        player))
                .orElse(null);

        if (!clan.hasPermission(player.getName(), ClanRank.MODERATOR)) {
            executeCommandsWithPlaceholders(fileConfig, "messages.no_permission", null);
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
            executeCommandsWithPlaceholders(fileConfig, "messages.max_limit_reached", null);
            return;
        }

        Stream.of("pvp_toggle", "set_base", "increase_limit", "shop_editor")
                .forEach(type -> {
                    ButtonConfig btnConfig = config.buttons().get(type);
                    Material material = type.equals("pvp_toggle")
                            ? getMaterial(pvpEnabled ? btnConfig.disabledMaterial() : btnConfig.enabledMaterial())
                            : getMaterial(btnConfig.material());
                    String display = Plugin.getWithColor().hexToMinecraftColor(type.equals("pvp_toggle")
                            ? (pvpEnabled ? btnConfig.disabledDisplay() : btnConfig.enabledDisplay())
                            : btnConfig.display());
                    List<String> lore = type.equals("pvp_toggle")
                            ? List.of(Plugin.getWithColor().hexToMinecraftColor(pvpEnabled ? btnConfig.disabledLore() : btnConfig.enabledLore()))
                            : btnConfig.lore().stream()
                            .map(line -> Plugin.getWithColor().hexToMinecraftColor(
                                    replacePlaceholders(line, null)
                                            .replace("%current_limit%", String.valueOf(currentLimit))
                                            .replace("%new_limit%", String.valueOf(newLimit))
                                            .replace("%cost%", String.valueOf(cost))))
                            .collect(Collectors.toList());

                    Button button = new Button(material)
                            .setDisplay(display)
                            .setLoreList(lore)
                            .disableInteract(false)
                            .withListener(event -> {
                                switch (type) {
                                    case "pvp_toggle" -> {
                                        clan.setPvpEnabled(!pvpEnabled);
                                        clan.save();
                                        executeCommandsWithPlaceholders(fileConfig, "buttons.pvp_toggle.commands", null);
                                        menu.refreshSlot(btnConfig.slot());
                                    }
                                    case "set_base" -> {
                                        Location location = player.getLocation();
                                        clan.setBaseLocation(location);
                                        clan.save();
                                        executeCommandsWithPlaceholders(fileConfig, "buttons.set_base.commands", location);
                                    }
                                    case "increase_limit" -> {
                                        VaultEconomyProvider economy = new VaultEconomyProvider();
                                        if (!economy.isAvailable() || economy.getBalance(player) < cost) {
                                            executeCommandsWithPlaceholders(fileConfig, "buttons.increase_limit.error_commands", null);
                                            return;
                                        }
                                        economy.withdraw(player, cost);
                                        clan.setMemberLimit(newLimit);
                                        clan.save();
                                        executeCommandsWithPlaceholders(fileConfig, "buttons.increase_limit.success_commands", null);
                                    }
                                    case "shop_editor" -> {
                                        if (!player.isOp()) {
                                            executeCommandsWithPlaceholders(fileConfig, "messages.no_permission", null);
                                            return;
                                        }
                                        new ClanShopEditorMenu(filesManager).openItemsMenu(player);
                                        executeCommandsWithPlaceholders(fileConfig, "buttons.shop_editor.commands", null);
                                    }
                                }
                            });
                    menu.setSlot(btnConfig.slot(), button);
                    menu.refreshItems();
                });

        menu.open(player);
    }

    private void executeCommandsWithPlaceholders(FileConfiguration config, String path, Location location) {
        List<String> commands = config.getStringList(path);
        if (commands.isEmpty()) {
            String singleCommand = config.getString(path);
            if (singleCommand != null && !singleCommand.isEmpty()) {
                commands = List.of(singleCommand);
            }
        }
        if (commands.isEmpty()) return;

        List<String> processedCommands = commands.stream()
                .map(command -> replacePlaceholders(command, location))
                .collect(Collectors.toList());

        RequirementExecute.execute(processedCommands, player);
    }

    private String replacePlaceholders(String input, Location location) {
        String result = input
                .replace("{clan_name}", clan.getName())
                .replace("{player_name}", player.getName())
                .replace("{clan_pvp_status}", clan.isPvpEnabled() ? "Включено" : "Выключено")
                .replace("{new_limit}", String.valueOf(clan.getMemberLimit() + config.memberLimitIncreaseStep()))
                .replace("{clan_member_limit}", String.valueOf(clan.getMemberLimit()))
                .replace("{clan_members}", String.valueOf(clan.getMembers().size()));
        if (location != null) {
            result = result
                    .replace("%x%", String.valueOf(location.getBlockX()))
                    .replace("%y%", String.valueOf(location.getBlockY()))
                    .replace("%z%", String.valueOf(location.getBlockZ()));
        }
        return Plugin.getWithColor().hexToMinecraftColor(result);
    }

    private Material getMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.BARRIER;
        }
    }

    private record SettingsConfig(
            String title,
            int size,
            String noPermission,
            int memberLimitIncreaseStep,
            double memberLimitIncreaseCost,
            int maxMemberLimit,
            String maxLimitReached,
            List<String> pvpToggleCommands,
            List<String> setBaseCommands,
            List<String> increaseLimitSuccessCommands,
            List<String> increaseLimitErrorCommands,
            List<String> shopEditorCommands,
            Map<String, ButtonConfig> buttons,
            Material fillerMaterial,
            List<Integer> fillerSlots
    ) {
        public SettingsConfig {
            if (title == null || noPermission == null || maxLimitReached == null || pvpToggleCommands == null ||
                    setBaseCommands == null || increaseLimitSuccessCommands == null || increaseLimitErrorCommands == null ||
                    shopEditorCommands == null || fillerMaterial == null || fillerSlots == null) {
                throw new IllegalArgumentException("Required configuration values are missing in clan_settings.yml");
            }
            if (size < 1 || size > 6) {
                throw new IllegalArgumentException("Menu size must be between 1 and 6 rows");
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
            String material,
            String display,
            List<String> lore
    ) {
        public ButtonConfig {
            if (slot < 0) {
                throw new IllegalArgumentException("Slot must be non-negative for button configuration");
            }
        }
    }
}