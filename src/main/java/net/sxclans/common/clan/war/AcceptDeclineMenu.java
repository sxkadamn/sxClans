package net.sxclans.common.clan.war;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.sxclans.bukkit.Depend;
import net.sxclans.bukkit.files.FilesManagerInterface;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class AcceptDeclineMenu {

    private final Player player;
    private final String senderClanName;
    private final String receiverClanName;
    private final AnimatedMenu menu;
    private final AcceptDeclineConfig config;

    public AcceptDeclineMenu(Player player, String senderClanName, String receiverClanName, FilesManagerInterface filesManager) {
        this.player = player;
        this.senderClanName = senderClanName;
        this.receiverClanName = receiverClanName;
        FileConfiguration fileConfig = filesManager.getMenuConfig("accept_decline");

        this.config = new AcceptDeclineConfig(
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("title")),
                fileConfig.getInt("size"),
                Material.valueOf(fileConfig.getString("filler.material")),
                fileConfig.getIntegerList("filler.slots"),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.accept.display")),
                Material.valueOf(fileConfig.getString("buttons.accept.material")),
                fileConfig.getInt("buttons.accept.slot"),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.decline.display")),
                Material.valueOf(fileConfig.getString("buttons.decline.material")),
                fileConfig.getInt("buttons.decline.slot"),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("messages.accepted_war_request")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("messages.declined_war_request"))
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

        String acceptDisplay = config.acceptDisplay().replace("{clan}", senderClanName);
        menu.setSlot(config.acceptSlot(), new Button(config.acceptMaterial())
                .setDisplay(acceptDisplay)
                .withListener(event -> {
                    Depend.getClanManage().acceptWarRequest(receiverClanName);
                    String message = config.acceptedMessage().replace("{clan}", senderClanName);
                    player.sendMessage(message);
                    player.closeInventory();
                }));

        String declineDisplay = config.declineDisplay().replace("{clan}", senderClanName);
        menu.setSlot(config.declineSlot(), new Button(config.declineMaterial())
                .setDisplay(declineDisplay)
                .withListener(event -> {
                    Depend.getClanManage().cancelWarRequest(receiverClanName);
                    String message = config.declinedMessage().replace("{clan}", senderClanName);
                    player.sendMessage(message);
                    player.closeInventory();
                }));

        menu.open(player);
    }

    private record AcceptDeclineConfig(
            String name,
            int size,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            String acceptDisplay,
            Material acceptMaterial,
            int acceptSlot,
            String declineDisplay,
            Material declineMaterial,
            int declineSlot,
            String acceptedMessage,
            String declinedMessage
    ) {
        public AcceptDeclineConfig {
            if (name == null || size <= 0 || fillerMaterial == null || fillerSlots == null || fillerSlots.isEmpty() ||
                    acceptDisplay == null || acceptMaterial == null || declineDisplay == null || declineMaterial == null ||
                    acceptedMessage == null || declinedMessage == null) {
                throw new IllegalArgumentException("Required configuration values are missing in accept_decline.yml");
            }
        }
    }
}