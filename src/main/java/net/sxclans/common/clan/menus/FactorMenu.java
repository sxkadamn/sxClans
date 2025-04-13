package net.sxclans.common.clan.menus;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.sxclans.bukkit.Depend;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.Utility;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.menus.economy.DepositMenu;
import net.sxclans.common.clan.menus.economy.WithdrawMenu;
import net.sxclans.common.clan.menus.interactions.InviteMenu;
import net.sxclans.common.clan.menus.interactions.SettingsMenu;
import net.sxclans.common.clan.menus.management.ShopManageMenu;
import net.sxclans.common.clan.models.ClanRank;
import net.sxclans.common.clan.war.WarMenu;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class FactorMenu {
    private final Player player;
    private final Clan clan;
    private final FilesManagerInterface filesManager;

    public FactorMenu(Player player, FilesManagerInterface filesManager) {
        this.player = player;
        this.clan = Depend.getClanManage().getMembersClan(player.getName());
        this.filesManager = filesManager;
    }

    public void open() {
        FileConfiguration config = filesManager.getMenuConfig("main");
        String menuName = Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.name"));
        int menuSize = config.getInt("menu.size");
        AnimatedMenu menu = Plugin.getMenuManager().createMenuFromConfig(menuName, menuSize, player);

        Material fillerMaterial = getMaterial(config, "menu.filler.material");
        List<Integer> fillerSlots = config.getIntegerList("menu.filler.slots");
        for (int slot : fillerSlots) {
            if (slot >= 0 && slot < menuSize * 9) {
                Button fillerButton = new Button(fillerMaterial)
                        .setDisplay(" ")
                        .disableInteract(true)
                        .applyMeta(itemMeta -> itemMeta);
                menu.setSlot(slot, fillerButton);
            }
        }

        Button mainButton = new Button(getMaterial(config, "menu.item"));
        mainButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.name")));
        mainButton.setLoreList(processLore(config.getStringList("menu.lore")));
        mainButton.disableInteract(false);
        menu.setSlot(config.getInt("menu.main_slot", 13), mainButton);

        boolean isLeader = clan.getLeader().equals(player.getName());
        Button leaveButton = new Button(getMaterial(config, "menu.buttons.leave.item"));
        leaveButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(
                isLeader ? config.getString("menu.buttons.leave.leader_display") : config.getString("menu.buttons.leave.member_display")
        ));
        leaveButton.disableInteract(false);
        leaveButton.withListener(event -> {
            if (isLeader) {
                Depend.getClanManage().removeClan(clan);
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.leave.leader_message")));
            } else {
                clan.removeMember(player.getName());
                Utility.playConfiguredSounds(player, "sounds.clan_left");
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.leave.member_message")));
            }
            player.closeInventory();
        });
        menu.setSlot(config.getInt("menu.buttons.leave.slot", 46), leaveButton);

        if (isLeader) {
            Button inviteButton = new Button(getMaterial(config, "menu.buttons.invite.item"));
            inviteButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.invite.display")));
            inviteButton.disableInteract(false);
            inviteButton.withListener(event -> new InviteMenu(player, filesManager).open());
            menu.setSlot(config.getInt("menu.buttons.invite.slot", 20), inviteButton);
        }

        Button clanShopButton = new Button(getMaterial(config, "menu.buttons.clan_shop.item"));
        clanShopButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.clan_shop.display")));
        clanShopButton.disableInteract(false);
        clanShopButton.withListener(event -> new ShopManageMenu(player, clan, filesManager).open());
        menu.setSlot(config.getInt("menu.buttons.clan_shop.slot"), clanShopButton);

        Button depositButton = new Button(getMaterial(config, "menu.buttons.deposit.item"));
        depositButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.deposit.display")));
        depositButton.disableInteract(false);
        depositButton.withListener(event -> new DepositMenu(filesManager).openDepositGUI(player, clan));
        menu.setSlot(config.getInt("menu.buttons.deposit.slot"), depositButton);

        Button withdrawButton = new Button(getMaterial(config, "menu.buttons.withdraw.item"));
        withdrawButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.withdraw.display")));
        withdrawButton.disableInteract(false);
        withdrawButton.withListener(event -> new WithdrawMenu(filesManager).openWithdrawGUI(player, clan));
        menu.setSlot(config.getInt("menu.buttons.withdraw.slot"), withdrawButton);

        Button membersButton = new Button(getMaterial(config, "menu.buttons.members.item"));
        membersButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.members.display")));
        membersButton.withListener(event -> new MembersMenu(player, filesManager).open());
        membersButton.disableInteract(false);
        menu.setSlot(config.getInt("menu.buttons.members.slot"), membersButton);

        if (clan.hasPermission(player.getName(), ClanRank.MODERATOR)) {
            Button settingsButton = new Button(getMaterial(config, "menu.buttons.settings.item"));
            settingsButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.settings.display")));
            settingsButton.disableInteract(false);
            settingsButton.withListener(event -> new SettingsMenu(player, clan, filesManager).open());
            menu.setSlot(config.getInt("menu.buttons.settings.slot"), settingsButton);
        }

        Button teleportButton = new Button(getMaterial(config, "menu.buttons.teleport_base.item"));
        teleportButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.teleport_base.display")));
        teleportButton.disableInteract(false);
        teleportButton.withListener(event -> {
            if (clan.getBaseLocation() != null) {
                player.teleport(clan.getBaseLocation());
                Utility.playConfiguredSounds(player, "sounds.clan_teleport");
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.teleport_base.success_message")));
                player.closeInventory();
            } else {
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.teleport_base.no_base_message")));
            }
        });
        menu.setSlot(config.getInt("menu.buttons.teleport_base.slot"), teleportButton);

        Button warButton = new Button(getMaterial(config, "menu.buttons.war.item"));
        warButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.war.display")));
        warButton.disableInteract(false);
        warButton.withListener(event -> {
            if (clan.isInWar()) {
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("&cВаш клан уже участвует в войне."));
                return;
            }
            new WarMenu(player, filesManager).open();
        });
        menu.setSlot(config.getInt("menu.buttons.war.slot"), warButton);

        menu.open(player);
    }

    private Material getMaterial(FileConfiguration config, String path) {
        try {
            return Material.valueOf(config.getString(path, "BARRIER").toUpperCase());
        } catch (IllegalArgumentException e) {
            return Material.BARRIER;
        }
    }

    private List<String> processLore(List<String> lore) {
        return lore.stream()
                .map(line -> Plugin.getWithColor().hexToMinecraftColor(line
                        .replace("{clan_name}", clan.getName())
                        .replace("{yours_rank}", clan.getRank(player.getName()).getDisplayName())
                        .replace("{clan_lvl}", String.valueOf(clan.getLevel()))
                        .replace("{clan_members}", String.valueOf(clan.getMembers().size()))
                        .replace("{clan_moderators}", String.valueOf(clan.countMembersByRank(ClanRank.MODERATOR)))
                        .replace("{clan_leaders}", String.valueOf(clan.countMembersByRank(ClanRank.LEADER)))
                        .replace("{clan_co_leaders}", String.valueOf(clan.countMembersByRank(ClanRank.CO_LEADER)))
                        .replace("{clan_balance}", String.valueOf(clan.getBank()))
                        .replace("{clan_balance_rub}", String.valueOf(clan.getRubles()))
                        .replace("{clan_pvp_status}", clan.isPvpEnabled() ? "&aВключено" : "&cВыключено")
                        .replace("{clan_member_limit}", String.valueOf(clan.getMemberLimit()))
                        .replace("{clan_war_status}", clan.isInWar() ? "&cУчаствует в войне" : "&aМир")))
                .toList();
    }

}
