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
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("button.display"))
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

        Map<String, Clan> clanMap = Depend.getClanManage().getClans();
        int slot = 0;

        for (Clan clan : clanMap.values()) {
            if (clan.getName().equals(Depend.getClanManage().getMembersClan(player.getName()).getName())) {
                continue;
            }

            String displayText = config.buttonDisplay().replace("{clan}", clan.getName());

            menu.setSlot(slot, new Button(Material.RED_BANNER)
                    .setDisplay(displayText)
                    .withListener(event -> {
                        Depend.getClanManage().getMembersClan(player.getName()).sendWarRequest(clan);
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("&aЗапрос на войну отправлен клану &e" + clan.getName()));
                    }));
            slot++;
        }

        menu.open(player);
    }

    private record EnemyClanConfig(
            String name,
            int size,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            String buttonDisplay
    ) {
        public EnemyClanConfig {
            if (name == null || size <= 0 || fillerMaterial == null || fillerSlots == null || fillerSlots.isEmpty() || buttonDisplay == null) {
                throw new IllegalArgumentException("Required configuration values are missing in enemy_clan.yml");
            }
        }
    }
}