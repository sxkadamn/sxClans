package net.sxclans.common.clan.integration;

import org.bukkit.OfflinePlayer;

public interface EconomyProvider {
    boolean has(OfflinePlayer player, double amount);
    double getBalance(OfflinePlayer player);
    void setBalance(OfflinePlayer player, double amount);
    void deposit(OfflinePlayer player, double amount);
    void withdraw(OfflinePlayer player, double amount);
    boolean isAvailable();
}