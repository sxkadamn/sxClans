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

public class SelectEnemyClanMenu {

    private final Player player;
    private final AnimatedMenu menu;
    private final EnemyClanConfig config;

    public SelectEnemyClanMenu(Player player, FilesManagerInterface filesManager) {
        this.player = player;
        FileConfiguration fileConfig = filesManager.getMenuConfig("enemy_clan");

        this.config = new EnemyClanConfig(
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("title")),
                fileConfig.getInt("size"),
                Material.valueOf(fileConfig.getString("filler.material")),
                fileConfig.getIntegerList("filler.slots"),
                fileConfig.getIntegerList("button_slots"),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("button.display")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("messages.war_request_sent"))
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

        Map<String, Clan> clanMap = Depend.getClanManage().getClans();
        List<Integer> buttonSlots = config.buttonSlots();
        int index = 0;

        for (Clan clan : clanMap.values()) {
            if (clan.getName().equals(Depend.getClanManage().getMembersClan(player.getName()).getName())) {
                continue;
            }
            if (index >= buttonSlots.size()) {
                break;
            }

            int slot = buttonSlots.get(index);

            String displayText = config.buttonDisplay().replace("{clan}", clan.getName());

            menu.setSlot(slot, new Button(Material.RED_BANNER)
                    .setDisplay(displayText)
                    .withListener(event -> {
                        Depend.getClanManage().getMembersClan(player.getName()).sendWarRequest(clan);
                        String message = config.warRequestSentMessage().replace("{clan}", clan.getName());
                        player.sendMessage(message);
                    }));

            index++;
        }

        menu.open(player);
    }

    private record EnemyClanConfig(
            String name,
            int size,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            List<Integer> buttonSlots,
            String buttonDisplay,
            String warRequestSentMessage
    ) {
        public EnemyClanConfig {
            if (name == null || size <= 0 || fillerMaterial == null || fillerSlots == null || fillerSlots.isEmpty() ||
                    buttonSlots == null || buttonSlots.isEmpty() || buttonDisplay == null || warRequestSentMessage == null) {
                throw new IllegalArgumentException("Required configuration values are missing in enemy_clan.yml");
            }
        }
    }
}