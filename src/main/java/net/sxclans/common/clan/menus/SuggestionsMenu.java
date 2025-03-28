package net.sxclans.common.clan.menus;

import net.lielibrary.bukkit.Plugin;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.Utility;
import net.sxclans.common.clan.integration.VaultEconomyProvider;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.sxclans.common.clan.Clan;
import net.sxclans.bukkit.Depend;

import java.util.*;
import java.util.stream.Stream;

public class SuggestionsMenu {

    private final SuggestionsConfig config;

    public SuggestionsMenu(FilesManagerInterface filesManager) {
        FileConfiguration fileConfig = filesManager.getMenuConfig("create_clan");
        this.config = new SuggestionsConfig(
                fileConfig.getString("title"),
                fileConfig.getString("input_text"),
                fileConfig.getString("icon"),
                fileConfig.getDouble("cost"),
                fileConfig.getInt("max_length"),
                fileConfig.getString("errors.empty_name"),
                fileConfig.getString("errors.economy_unavailable"),
                fileConfig.getString("errors.insufficient_funds"),
                fileConfig.getString("errors.name_too_long"),
                fileConfig.getString("errors.invalid_chars"),
                fileConfig.getString("errors.clan_exists"),
                fileConfig.getString("success_message")
        );
    }

    public void openAnvilGUI(Player player) {
        if (config == null) return;

        VaultEconomyProvider economy = new VaultEconomyProvider();
        boolean economyAvailable = economy.isAvailable();
        double playerBalance = economy.getBalance(player);

        new AnvilGUI.Builder()
                .plugin(Depend.getInstance())
                .title(Plugin.getWithColor().hexToMinecraftColor(config.title()))
                .text(Plugin.getWithColor().hexToMinecraftColor(config.inputText()))
                .itemLeft(new ItemStack(Material.valueOf(config.icon().toUpperCase())))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();

                    String clanName = stateSnapshot.getText().trim();

                    return Stream.of(
                                    Map.entry(clanName.isEmpty(), config.errorEmptyName()),
                                    Map.entry(!economyAvailable, config.errorEconomyUnavailable()),
                                    Map.entry(playerBalance < config.cost(), config.errorInsufficientFunds()),
                                    Map.entry(clanName.length() > config.maxLength(), config.errorNameTooLong()),
                                    Map.entry(!clanName.matches("[a-zA-Z0-9_]+"), config.errorInvalidChars())
                            )
                            .filter(Map.Entry::getKey)
                            .map(Map.Entry::getValue)
                            .filter(Objects::nonNull)
                            .map(error -> AnvilGUI.ResponseAction.replaceInputText(
                                    Plugin.getWithColor().hexToMinecraftColor(error)))
                            .findFirst()
                            .map(Collections::singletonList)
                            .orElseGet(() -> Optional.ofNullable(Depend.getClanManage().getClan(clanName))
                                    .map(existingClan -> {
                                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.errorClanExists()));
                                        return List.of(AnvilGUI.ResponseAction.close());
                                    })
                                    .orElseGet(() -> {
                                        Clan newClan = new Clan(clanName, player.getName());
                                        newClan.addMember(player.getName());
                                        economy.withdraw(player, config.cost());
                                        newClan.save();
                                        Depend.getClanManage().saveClan(newClan);
                                        Utility.playConfiguredSounds(player, "sounds.clan_created");
                                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                                config.successMessage().replace("{clan_name}", clanName)));
                                        return List.of(AnvilGUI.ResponseAction.close());
                                    }));
                })
                .open(player);
    }

    private record SuggestionsConfig(
            String title,
            String inputText,
            String icon,
            double cost,
            int maxLength,
            String errorEmptyName,
            String errorEconomyUnavailable,
            String errorInsufficientFunds,
            String errorNameTooLong,
            String errorInvalidChars,
            String errorClanExists,
            String successMessage
    ) {
        public SuggestionsConfig {
            if (title == null || inputText == null || icon == null ||
                    errorEmptyName == null || errorEconomyUnavailable == null ||
                    errorInsufficientFunds == null || errorNameTooLong == null ||
                    errorInvalidChars == null || errorClanExists == null || successMessage == null) {
                throw new IllegalArgumentException("Required configuration values are missing in create_clan.yml");
            }
        }
    }
}
