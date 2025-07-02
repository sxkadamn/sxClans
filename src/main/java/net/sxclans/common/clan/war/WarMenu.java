package net.sxclans.common.clan.war;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.sxclans.bukkit.Depend;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class WarMenu {

    private final Player player;
    private final AnimatedMenu menu;
    private final WarConfig config;

    public WarMenu(Player player, FilesManagerInterface filesManager) {
        this.player = player;
        FileConfiguration fileConfig = filesManager.getMenuConfig("war");

        this.config = new WarConfig(
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("title")),
                fileConfig.getInt("size"),
                Material.valueOf(fileConfig.getString("filler.material")),
                fileConfig.getIntegerList("filler.slots"),

                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.send_war_request.display")),
                Material.valueOf(fileConfig.getString("buttons.send_war_request.material")),
                fileConfig.getInt("buttons.send_war_request.slot"),

                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.select_participants.display")),
                Material.valueOf(fileConfig.getString("buttons.select_participants.material")),
                fileConfig.getInt("buttons.select_participants.slot"),

                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.current_wars.display")),
                Material.valueOf(fileConfig.getString("buttons.current_wars.material")),
                fileConfig.getInt("buttons.current_wars.slot"),

                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.war_invitations.display")),
                Material.valueOf(fileConfig.getString("buttons.war_invitations.material")),
                fileConfig.getInt("buttons.war_invitations.slot"),

                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("messages.permission"))
        );

        this.menu = Plugin.getMenuManager().createMenuFromConfig(
                config.name(),
                config.size(), player);
    }

    public void open() {
        if (menu == null || config == null) return;

        Clan clan = Depend.getClanManage().getMembersClan(player.getName());
        ClanRank rank = (clan != null) ? clan.getRank(player.getName()) : null;
        boolean hasAccess = rank != null && rank.getPower() >= ClanRank.CO_LEADER.getPower();

        for (int slot : config.fillerSlots()) {
            if (slot >= 0 && slot < config.size() * 9) {
                menu.setSlot(slot, new Button(config.fillerMaterial())
                        .setDisplay(" ")
                        .disableInteract(true));
            }
        }

        menu.setSlot(config.sendWarRequestSlot(), new Button(config.sendWarRequestMaterial())
                .setDisplay(config.sendWarRequestDisplay())
                .withListener(event -> {
                    if (!hasAccess) {
                        player.sendMessage(config.noPermissionMessage());
                        return;
                    }
                    new SelectEnemyClanMenu(player, Depend.getInstance().getFilesManager()).open();
                }));


        menu.setSlot(config.selectParticipantsSlot(), new Button(config.selectParticipantsMaterial())
                .setDisplay(config.selectParticipantsDisplay())
                .withListener(event -> {
                    if (!hasAccess) {
                        player.sendMessage(config.noPermissionMessage());
                        return;
                    }
                    new SelectWarParticipantsMenu(player, Depend.getInstance().getFilesManager()).open();
                }));

        menu.setSlot(config.currentWarsSlot(), new Button(config.currentWarsMaterial())
                .setDisplay(config.currentWarsDisplay())
                .withListener(event -> {
                    player.sendMessage("§c[Клан] §fПросмотр текущих войн еще не реализован.");
                }));

        menu.setSlot(config.warInvitationsSlot(), new Button(config.warInvitationsMaterial())
                .setDisplay(config.warInvitationsDisplay())
                .withListener(event -> {
                    if (!hasAccess) {
                        player.sendMessage(config.noPermissionMessage());
                        return;
                    }
                    new WarInvitationsMenu(player, Depend.getInstance().getFilesManager()).open();
                }));

        menu.open(player);
    }


    private record WarConfig(
            String name,
            int size,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            String sendWarRequestDisplay,
            Material sendWarRequestMaterial,
            int sendWarRequestSlot,
            String selectParticipantsDisplay,
            Material selectParticipantsMaterial,
            int selectParticipantsSlot,
            String currentWarsDisplay,
            Material currentWarsMaterial,
            int currentWarsSlot,
            String warInvitationsDisplay,
            Material warInvitationsMaterial,
            int warInvitationsSlot,
            String noPermissionMessage
    ) {
        public WarConfig {
            if (name == null || fillerMaterial == null || fillerSlots == null ||
                    sendWarRequestDisplay == null || sendWarRequestMaterial == null ||
                    selectParticipantsDisplay == null || selectParticipantsMaterial == null ||
                    currentWarsDisplay == null || currentWarsMaterial == null ||
                    warInvitationsDisplay == null || warInvitationsMaterial == null ||
                    noPermissionMessage == null) {
                throw new IllegalArgumentException("Required configuration values are missing in war.yml");
            }
        }
    }
}