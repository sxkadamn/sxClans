package net.sxclans.common.clan.base;

import net.sxclans.common.clan.Clan;

import java.util.HashMap;

public interface ClanBaseManager {
    void loadClans();

    void saveClans();


    void saveClan(Clan paramClan);

    void removeClan(Clan paramClan);

    HashMap<String, Clan> getClans();

    Clan getClan(String paramString);

    Clan getOwnersClan(String paramString);

    Clan getMembersClan(String paramString);

}