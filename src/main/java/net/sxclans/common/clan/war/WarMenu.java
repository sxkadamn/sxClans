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
import java.util.Optional;

public class WarMenu {

    private final Player player;
    private final AnimatedMenu menu;
    private final WarConfig config;

    public WarMenu(Player player, FilesManagerInterface filesManager) {
        this.player = player;
        FileConfiguration fileConfig = filesManager.getMenuConfig("war");

        this.config = new WarConfig(
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("title")),
                fileConfig.getInt("size", 3),
                Material.valueOf(fileConfig.getString("filler.material")),
                fileConfig.getIntegerList("filler.slots"),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.send_war_request.display")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.select_participants.display")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.current_wars.display")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("buttons.war_invitations.display"))
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

        menu.setSlot(10, new Button(Material.IRON_SWORD)
                .setDisplay(config.sendWarRequestDisplay())
                .withListener(event -> {
                    new SelectEnemyClanMenu(player, Depend.getInstance().getFilesManager()).open();
                }));

        menu.setSlot(12, new Button(Material.PLAYER_HEAD)
                .setDisplay(config.selectParticipantsDisplay())
                .withListener(event -> {
                    new SelectWarParticipantsMenu(player, Depend.getInstance().getFilesManager()).open();
                }));

        menu.setSlot(14, new Button(Material.BOOK)
                .setDisplay(config.currentWarsDisplay())
                .withListener(event -> {
                    player.sendMessage("§c[Клан] §fПросмотр текущих войн еще не реализован.");
                }));

        menu.setSlot(16, new Button(Material.PAPER)
                .setDisplay(config.warInvitationsDisplay())
                .withListener(event -> {
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
            String selectParticipantsDisplay,
            String currentWarsDisplay,
            String warInvitationsDisplay // Новое поле для кнопки приглашений
    ) {
        public WarConfig {
            if (name == null || fillerMaterial == null || fillerSlots == null ||
                    sendWarRequestDisplay == null || selectParticipantsDisplay == null ||
                    currentWarsDisplay == null || warInvitationsDisplay == null) {
                throw new IllegalArgumentException("Required configuration values are missing in war.yml");
            }
        }
    }
}