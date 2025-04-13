package net.sxclans.common.clan.base;

import net.sxclans.common.clan.Clan;

import java.util.Map;

public interface ClanBaseManager {
    void loadClans();

    void saveClans();


    void saveClan(Clan paramClan);

    void removeClan(Clan paramClan);

    Map<String, Clan> getClans();

    Clan getClan(String paramString);

    Clan getOwnersClan(String paramString);

    Clan getMembersClan(String paramString);

    void addWarRequest(String senderClanName, String receiverClanName);

    void acceptWarRequest(String receiverClanName);

    void cancelWarRequest(String receiverClanName);

    Map<String, String> getWarRequests();
}