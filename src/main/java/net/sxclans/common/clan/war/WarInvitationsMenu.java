package net.sxclans.common.clan.war;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.sxclans.bukkit.Depend;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;

public class WarInvitationsMenu {

    private final Player player;
    private final AnimatedMenu menu;
    private final WarInvitationsConfig config;

    public WarInvitationsMenu(Player player, FilesManagerInterface filesManager) {
        this.player = player;
        FileConfiguration fileConfig = filesManager.getMenuConfig("war_invitations");

        this.config = new WarInvitationsConfig(
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("title")),
                fileConfig.getInt("size"),
                Material.valueOf(fileConfig.getString("filler.material")),
                fileConfig.getIntegerList("filler.slots"),
                fileConfig.getIntegerList("button_slots"),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("button.clan_request.display")),
                Material.valueOf(fileConfig.getString("button.clan_request.material")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("button.accept.display")),
                Material.valueOf(fileConfig.getString("button.accept.material")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("button.decline.display")),
                Material.valueOf(fileConfig.getString("button.decline.material"))
        );

        this.menu = Plugin.getMenuManager().createMenuFromConfig(
                config.name(),
                config.size(), player);
    }

    public void open() {
        if (menu == null || config == null) return;

        for (int slot : config.fillerSlots()) {
            if (slot >= 0 && slot < config.size() * 9) {
                menu.setSlot(slot, new Button(config.fillerMaterial())
                        .setDisplay(" ")
                        .disableInteract(true));
            }
        }

        Clan playerClan = Depend.getClanManage().getMembersClan(player.getName());
        Map<String, String> warRequests = Depend.getClanManage().getWarRequests();

        List<Integer> buttonSlots = config.buttonSlots();
        int index = 0;

        for (Map.Entry<String, String> entry : warRequests.entrySet()) {
            String receiverClanName = entry.getKey();
            String senderClanName = entry.getValue();

            if (!receiverClanName.equals(playerClan.getName())) {
                continue;
            }

            if (index >= buttonSlots.size()) {
                break;
            }

            int slot = buttonSlots.get(index);

            String clanRequestDisplay = config.clanRequestDisplay().replace("{clan}", senderClanName);
            menu.setSlot(slot, new Button(config.clanRequestMaterial())
                    .setDisplay(clanRequestDisplay)
                    .applyMeta(meta -> {
                        if (meta instanceof SkullMeta skullMeta) {
                            skullMeta.setOwningPlayer(Depend.getInstance().getServer().getOfflinePlayer(senderClanName));
                        }
                        return meta;
                    })
                    .withListener(event -> {
                        new AcceptDeclineMenu(player, senderClanName, receiverClanName, Depend.getInstance().getFilesManager()).open();
                    }));

            index++;
        }

        menu.open(player);
    }

    private record WarInvitationsConfig(
            String name,
            int size,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            List<Integer> buttonSlots,
            String clanRequestDisplay,
            Material clanRequestMaterial,
            String acceptDisplay,
            Material acceptMaterial,
            String declineDisplay,
            Material declineMaterial
    ) {
        public WarInvitationsConfig {
            if (name == null || size <= 0 || fillerMaterial == null || fillerSlots == null || fillerSlots.isEmpty() ||
                    buttonSlots == null || buttonSlots.isEmpty() || // Проверка новых слотов
                    clanRequestDisplay == null || clanRequestMaterial == null ||
                    acceptDisplay == null || acceptMaterial == null || declineDisplay == null || declineMaterial == null) {
                throw new IllegalArgumentException("Required configuration values are missing in war_invitations.yml");
            }
        }
    }
}