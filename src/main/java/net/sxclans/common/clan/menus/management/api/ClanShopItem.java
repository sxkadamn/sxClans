package net.sxclans.common.clan.menus.management.api;

import net.sxclans.common.clan.Clan;

public interface ClanShopItem {
    boolean canBeViewedBy(Clan clan, int requiredLevel);
}