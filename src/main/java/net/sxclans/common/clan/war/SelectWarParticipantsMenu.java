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
import java.util.Optional;

public class SelectWarParticipantsMenu {

    private final Player player;
    private final Clan clan;
    private final AnimatedMenu menu;
    private final WarParticipantsConfig config;

    public SelectWarParticipantsMenu(Player player, FilesManagerInterface filesManager) {
        this.player = player;
        this.clan = Depend.getClanManage().getMembersClan(player.getName());
        FileConfiguration fileConfig = filesManager.getMenuConfig("war_participants");

        this.config = new WarParticipantsConfig(
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("name")),
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

        int slot = 0;
        for (String member : clan.getMembers()) {
            String displayText = config.buttonDisplay().replace("{player}", member);

            menu.setSlot(slot, new Button(Material.PLAYER_HEAD)
                    .setDisplay(displayText)
                    .withListener(event -> {
                        clan.addWarParticipant(member);
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("&aИгрок &e" + member + " &aдобавлен в участники войны."));
                    })
                    .applyMeta(meta -> {
                        if (meta instanceof SkullMeta skullMeta) {
                            skullMeta.setOwningPlayer(Depend.getInstance().getServer().getOfflinePlayer(member));
                        }
                        return meta;
                    }));
            slot++;
        }

        menu.open(player);
    }

    private record WarParticipantsConfig(
            String name,
            int size,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            String buttonDisplay
    ) {
        public WarParticipantsConfig {
            if (name == null || size <= 0 || fillerMaterial == null || fillerSlots == null || fillerSlots.isEmpty() || buttonDisplay == null) {
                throw new IllegalArgumentException("Required configuration values are missing in war_participants.yml");
            }
        }
    }
}