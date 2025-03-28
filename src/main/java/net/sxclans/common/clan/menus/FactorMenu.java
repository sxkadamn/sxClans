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
import net.sxclans.common.clan.models.ClanRank;
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
        String menuName = Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.name", "Меню клана"));
        int menuSize = config.getInt("menu.size");
        AnimatedMenu menu = Plugin.getMenuManager().createMenuFromConfig(menuName, menuSize, player);

        Button mainButton = new Button(getMaterial(config, "menu.item"));
        mainButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.name", "Кнопка")));
        mainButton.setLoreList(ssucksuckLore(config.getStringList("menu.lore")));
        mainButton.disableInteract(true);
        menu.setSlot(config.getInt("menu.main_slot", 13), mainButton);

        boolean isLeader = clan.getLeader().equals(player.getName());
        Button leaveButton = new Button(getMaterial(config, "menu.buttons.leave.item"));
        leaveButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(
                isLeader ? config.getString("menu.buttons.leave.leader_display", "Выйти") : config.getString("menu.buttons.leave.member_display", "Покинуть клан")
        ));
        leaveButton.disableInteract(true);
        leaveButton.withListener(event -> {
            if (isLeader) {
                Depend.getClanManage().removeClan(clan);
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.leave.leader_message", "Вы распустили клан.")));
            } else {
                clan.removeMember(player.getName());
                Utility.playConfiguredSounds(player, "sounds.clan_left");
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.leave.member_message", "Вы покинули клан.")));
            }
            player.closeInventory();
        });
        menu.setSlot(config.getInt("menu.buttons.leave.slot", 22), leaveButton);

        if (isLeader) {
            Button inviteButton = new Button(getMaterial(config, "menu.buttons.invite.item"));
            inviteButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.invite.display", "Пригласить игрока")));
            inviteButton.disableInteract(true);
            inviteButton.withListener(event -> new InviteMenu(player, filesManager).open());
            menu.setSlot(config.getInt("menu.buttons.invite.slot", 30), inviteButton);
        }

        Button depositButton = new Button(getMaterial(config, "menu.buttons.deposit.item"));
        depositButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.deposit.display", "Пополнить банк")));
        depositButton.disableInteract(true);
        depositButton.withListener(event -> new DepositMenu(filesManager).openDepositGUI(player, clan));
        menu.setSlot(config.getInt("menu.buttons.deposit.slot"), depositButton);

        Button withdrawButton = new Button(getMaterial(config, "menu.buttons.withdraw.item"));
        withdrawButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.withdraw.display", "Снять деньги")));
        withdrawButton.disableInteract(true);
        withdrawButton.withListener(event -> new WithdrawMenu(filesManager).openWithdrawGUI(player, clan));
        menu.setSlot(config.getInt("menu.buttons.withdraw.slot"), withdrawButton);

        Button membersButton = new Button(getMaterial(config, "menu.buttons.members.item"));
        membersButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.members.display", "Список членов клана")));
        membersButton.withListener(event -> new MembersMenu(player, filesManager).open());
        membersButton.disableInteract(true);
        menu.setSlot(config.getInt("menu.buttons.members.slot"), membersButton);

        if (clan.hasPermission(player.getName(), ClanRank.MODERATOR)) {
            Button settingsButton = new Button(getMaterial(config, "menu.buttons.settings.item"));
            settingsButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.settings.display", "Настройки клана")));
            settingsButton.disableInteract(true);
            settingsButton.withListener(event -> new SettingsMenu(player, clan, filesManager).open());
            menu.setSlot(config.getInt("menu.buttons.settings.slot"), settingsButton);
        }

        Button teleportButton = new Button(getMaterial(config, "menu.buttons.teleport_base.item"));
        teleportButton.setDisplay(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.teleport_base.display", "Телепорт к базе")));
        teleportButton.disableInteract(true);
        teleportButton.withListener(event -> {
            if (clan.getBaseLocation() != null) {
                player.teleport(clan.getBaseLocation());
                Utility.playConfiguredSounds(player, "sounds.clan_teleport");
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.teleport_base.success_message", "Вы телепортированы!")));
                player.closeInventory();
            } else {
                player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("menu.buttons.teleport_base.no_base_message", "База не установлена.")));
            }
        });
        menu.setSlot(config.getInt("menu.buttons.teleport_base.slot"), teleportButton);

        menu.open(player);
    }

    private Material getMaterial(FileConfiguration config, String path) {
        return Material.valueOf(config.getString(path, "BARRIER").toUpperCase());
    }

    private List<String> ssucksuckLore(List<String> lore) {
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
                        .replace("{clan_member_limit}", String.valueOf(clan.getMemberLimit()))))
                .toList();
    }
}
