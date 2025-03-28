package net.sxclans.common.clan.integration;


import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import org.bukkit.OfflinePlayer;

public class VaultEconomyProvider implements EconomyProvider {
    private final Economy economy;

    public VaultEconomyProvider() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        this.economy = rsp != null ? rsp.getProvider() : null;
        if (this.economy == null) {
            Bukkit.getLogger().warning("Vault economy provider is not available. Economic features will be disabled.");
        }
    }

    private Economy getEconomy() {
        if (economy == null) {
            throw new IllegalStateException("Vault economy provider is not initialized.");
        }
        return economy;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getEconomy().has(player, amount);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return getEconomy().getBalance(player);
    }

    @Override
    public void setBalance(OfflinePlayer player, double amount) {
        double currentBalance = getBalance(player);
        double difference = amount - currentBalance;
        if (difference > 0) {
            deposit(player, difference);
        } else if (difference < 0) {
            withdraw(player, -difference);
        }
    }

    @Override
    public void deposit(OfflinePlayer player, double amount) {
        getEconomy().depositPlayer(player, amount);
    }

    @Override
    public void withdraw(OfflinePlayer player, double amount) {
        getEconomy().withdrawPlayer(player, amount);
    }

    public boolean isAvailable() {
        return economy != null;
    }
}