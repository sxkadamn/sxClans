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
import java.util.UUID;

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
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("title")),
                fileConfig.getInt("size"),
                Material.valueOf(fileConfig.getString("filler.material")),
                fileConfig.getIntegerList("filler.slots"),
                fileConfig.getIntegerList("button_slots"),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("button.display_add")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("button.display_remove")),
                fileConfig.getInt("max_participants"),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("messages.added_to_war")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("messages.removed_from_war")),
                Plugin.getWithColor().hexToMinecraftColor(fileConfig.getString("messages.max_participants_reached"))
                        .replace("{max}", String.valueOf(fileConfig.getInt("max_participants")))
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

        List<Integer> buttonSlots = config.buttonSlots();
        int index = 0;

        for (String member : clan.getMembers()) {
            if (index >= buttonSlots.size()) {
                break;
            }

            int slot = buttonSlots.get(index);

            boolean isParticipant = clan.isWarParticipant(member);
            String displayText = isParticipant
                    ? config.displayRemove().replace("{player}", member)
                    : config.displayAdd().replace("{player}", member);

            menu.setSlot(slot, new Button(Material.PLAYER_HEAD)
                    .setDisplay(displayText)
                    .withListener(event -> {
                        if (event.isRightClick()) {
                            Player target = Depend.getInstance().getServer().getPlayerExact(member);
                            if (target != null) {
                                player.openInventory(target.getInventory());
                            } else {
                                player.sendMessage("§cИгрок не в сети.");
                            }
                        } else {
                            if (isParticipant) {
                                clan.removeWarParticipant(member);
                                player.sendMessage(config.removedFromWarMessage().replace("{player}", member));
                            } else {
                                if (clan.getWarParticipants().size() >= config.maxParticipants()) {
                                    player.sendMessage(config.maxParticipantsReachedMessage());
                                    return;
                                }
                                clan.addWarParticipant(member);
                                player.sendMessage(config.addedToWarMessage().replace("{player}", member));
                            }
                            open();
                        }
                    })
                    .applyMeta(meta -> {
                        if (meta instanceof SkullMeta skullMeta) {
                            var offlinePlayer = Depend.getInstance().getServer().getOfflinePlayer(member);
                            if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                                skullMeta.setOwningPlayer(offlinePlayer);
                            }
                        }
                        return meta;
                    }));

            index++;
        }

        menu.open(player);
    }

    private record WarParticipantsConfig(
            String name,
            int size,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            List<Integer> buttonSlots,
            String displayAdd,
            String displayRemove,
            int maxParticipants, // Максимальное количество участников
            String addedToWarMessage,
            String removedFromWarMessage,
            String maxParticipantsReachedMessage
    ) {
        public WarParticipantsConfig {
            if (name == null || size <= 0 || fillerMaterial == null || fillerSlots == null || fillerSlots.isEmpty() ||
                    buttonSlots == null || buttonSlots.isEmpty() || displayAdd == null || displayRemove == null ||
                    addedToWarMessage == null || removedFromWarMessage == null || maxParticipantsReachedMessage == null) {
                throw new IllegalArgumentException("Required configuration values are missing in war_participants.yml");
            }
        }
    }
}