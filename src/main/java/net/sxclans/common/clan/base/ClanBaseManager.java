package net.sxclans.common.clan.base;

import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.war.manager.WarManager;
import org.bukkit.entity.Player;

import java.util.Map;

public interface ClanBaseManager {
    void loadClans();

    void saveClans();


    void saveClan(Clan paramClan);

    void removeClan(Clan paramClan);

    Map<String, Clan> getClans();

    Clan getClan(String paramString);

    Clan getOwnersClan(String paramString);

    void removeMember(Clan paramClan, String paramString);

    Clan getMembersClan(String paramString);

    void addWarRequest(String senderClanName, String receiverClanName);

    void acceptWarRequest(String receiverClanName);

    void cancelWarRequest(String receiverClanName);

    Map<String, String> getWarRequests();

    Map<String, WarManager> getActiveWars();

    WarManager getActiveWarManager(Player playerUUID);

    void setActiveWarManager(Clan clanA, Clan clanB, WarManager warManager);

    void clearWar(Clan clanA, Clan clanB);
}