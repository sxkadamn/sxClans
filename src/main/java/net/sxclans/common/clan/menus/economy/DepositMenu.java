package net.sxclans.common.clan.menus.economy;

import net.lielibrary.bukkit.Plugin;
import net.sxclans.bukkit.Depend;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.integration.VaultEconomyProvider;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Stream;

public class DepositMenu {
    private final DepositConfig config;
    private final VaultEconomyProvider economy;

    public DepositMenu(FilesManagerInterface filesManager) {
        FileConfiguration fileConfig = filesManager.getMenuConfig("deposit");

        this.economy = new VaultEconomyProvider();

        this.config = new DepositConfig(
                fileConfig.getString("title"),
                fileConfig.getString("input_text"),
                fileConfig.getString("icon"),
                fileConfig.getString("success_message"),
                fileConfig.getString("errors.empty_input"),
                fileConfig.getString("errors.invalid_number"),
                fileConfig.getString("errors.negative_number"),
                fileConfig.getString("errors.economy_unavailable"),
                fileConfig.getString("errors.insufficient_funds")
        );
    }

    public void openDepositGUI(Player player, Clan clan) {
        if (config == null) return;

        new AnvilGUI.Builder()
                .plugin(Depend.getInstance())
                .title(Plugin.getWithColor().hexToMinecraftColor(config.title()))
                .text(Plugin.getWithColor().hexToMinecraftColor(config.inputText()))
                .itemLeft(new ItemStack(Material.valueOf(config.icon().toUpperCase())))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String input = stateSnapshot.getText().trim();

                    Optional<Double> amountOpt = parseAmount(input);
                    return Stream.of(
                                    Map.entry(input.isEmpty(), config.errorEmptyInput()),
                                    Map.entry(amountOpt.isEmpty(), config.errorInvalidNumber()),
                                    Map.entry(amountOpt.map(amount -> amount <= 0).orElse(false), config.errorNegativeNumber()),
                                    Map.entry(!economy.isAvailable(), config.errorEconomyUnavailable()),
                                    Map.entry(amountOpt.map(amount -> economy.getBalance(player) < amount).orElse(false), config.errorInsufficientFunds())
                            )
                            .filter(Map.Entry::getKey)
                            .filter(entry -> entry.getValue() != null)
                            .map(Map.Entry::getValue)
                            .map(error -> AnvilGUI.ResponseAction.replaceInputText(
                                    Plugin.getWithColor().hexToMinecraftColor(error)))
                            .findFirst()
                            .map(Collections::singletonList)
                            .orElseGet(() -> {
                                double amount = amountOpt.get();
                                economy.withdraw(player, amount);
                                clan.addMoneyToBank(amount);
                                clan.save();

                                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                        config.successMessage().replace("%amount%", String.valueOf(amount))));

                                return List.of(AnvilGUI.ResponseAction.close());
                            });
                })
                .open(player);
    }

    private Optional<Double> parseAmount(String input) {
        try {
            double amount = Double.parseDouble(input);
            return Optional.of(amount);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private record DepositConfig(
            String title,
            String inputText,
            String icon,
            String successMessage,
            String errorEmptyInput,
            String errorInvalidNumber,
            String errorNegativeNumber,
            String errorEconomyUnavailable,
            String errorInsufficientFunds
    ) {
        public DepositConfig {
            if (title == null || inputText == null || icon == null || successMessage == null) {
                throw new IllegalArgumentException("Required configuration values are missing in deposit.yml");
            }
        }
    }
}
