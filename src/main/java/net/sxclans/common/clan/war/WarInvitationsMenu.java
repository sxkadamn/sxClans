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

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("button.accept.display")),
                Material.valueOf(fileConfig.getString("button.accept.material")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("button.decline.display")),
                Material.valueOf(fileConfig.getString("button.decline.material"))
        );

        this.menu = Optional.of(config)
                .map(cfg -> Plugin.getMenuManager().createMenuFromConfig(
                        cfg.name(),
                        cfg.size(), player))
                .orElse(null);
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

        int slot = 0;
        for (Map.Entry<String, String> entry : warRequests.entrySet()) {
            String receiverClanName = entry.getKey();
            String senderClanName = entry.getValue();

            if (!receiverClanName.equals(playerClan.getName())) {
                continue;
            }

            String acceptDisplay = config.acceptDisplay().replace("{clan}", senderClanName);
            menu.setSlot(slot, new Button(config.acceptMaterial())
                    .setDisplay(acceptDisplay)
                    .withListener(event -> {
                        Depend.getClanManage().acceptWarRequest(receiverClanName);
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("&aВы приняли запрос на войну от клана &e" + senderClanName));
                        player.closeInventory();
                    }));

            slot++;

            String declineDisplay = config.declineDisplay().replace("{clan}", senderClanName);
            menu.setSlot(slot, new Button(config.declineMaterial())
                    .setDisplay(declineDisplay)
                    .withListener(event -> {
                        Depend.getClanManage().cancelWarRequest(receiverClanName);
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("&cВы отклонили запрос на войну от клана &e" + senderClanName));
                        player.closeInventory();
                    }));

            slot++;
        }

        menu.open(player);
    }

    private record WarInvitationsConfig(
            String name,
            int size,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            String acceptDisplay,
            Material acceptMaterial,
            String declineDisplay,
            Material declineMaterial
    ) {
        public WarInvitationsConfig {
            if (name == null || size <= 0 || fillerMaterial == null || fillerSlots == null || fillerSlots.isEmpty() ||
                    acceptDisplay == null || acceptMaterial == null || declineDisplay == null || declineMaterial == null) {
                throw new IllegalArgumentException("Required configuration values are missing in war_invitations.yml");
            }
        }
    }
}