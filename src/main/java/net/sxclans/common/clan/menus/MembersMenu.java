package net.sxclans.common.clan.menus;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.lielibrary.gui.buttons.ButtonListener;
import net.sxclans.bukkit.Depend;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.menus.management.MemberManageMenu;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class MembersMenu {
    private final Player player;
    private final Clan clan;
    private final AnimatedMenu menu;
    private final FilesManagerInterface filesManager;
    private final MembersConfig config;

    public MembersMenu(Player player, FilesManagerInterface filesManager) {
        this.player = player;
        this.clan = Depend.getClanManage().getMembersClan(player.getName());
        this.filesManager = filesManager;
        FileConfiguration fileConfig = filesManager.getMenuConfig("members_menu");
        this.config = new MembersConfig(
                fileConfig.getString("title"),
                fileConfig.getInt("size"),
                fileConfig.getString("member_button.display"),
                fileConfig.getString("member_button.rank"),
                fileConfig.getString("member_button.online"),
                fileConfig.getString("online_status.online"),
                fileConfig.getString("online_status.offline"),
                Material.valueOf(fileConfig.getString("filler.material", "GRAY_STAINED_GLASS_PANE")),
                fileConfig.getIntegerList("filler.slots")
        );
        this.menu = clan != null
                ? Plugin.getMenuManager().createMenuFromConfig(
                Plugin.getWithColor().hexToMinecraftColor(config.title()),
                config.size(),
                player)
                : null;
    }

    public void open() {
        if (menu == null || config == null) return;

        for (int slot : config.fillerSlots()) {
            if (slot >= 0 && slot < config.size() * 9) {
                menu.setSlot(slot, new Button(config.fillerMaterial())
                        .setDisplay(" ")
                        .disableInteract(true)
                        .applyMeta(meta -> meta));
            }
        }

        boolean hasPermission = clan.hasPermission(player.getName(), ClanRank.MODERATOR);

        IntStream.range(0, clan.getMembers().size())
                .mapToObj(slot -> createButton(slot, clan.getMembers().get(slot), hasPermission))
                .filter(Objects::nonNull)
                .forEach(entry -> menu.setSlot(entry.getKey(), entry.getValue()));
        menu.refreshItems();

        menu.open(player);
    }

    private Map.Entry<Integer, Button> createButton(int slot, String memberName, boolean hasPermission) {
        if (memberName.equals(player.getName())) return null;

        OfflinePlayer target = Bukkit.getOfflinePlayer(memberName);
        ClanRank rank = clan.getRank(memberName);

        ButtonConfig buttonConfig = new ButtonConfig(
                slot,
                Material.PLAYER_HEAD,
                config.display().replace("{player}", memberName),
                config.rank().replace("{rank}", rank.getDisplayName()),
                config.online().replace("{status}", target.isOnline() ? config.onlineStatus() : config.offlineStatus())
        );

        Consumer<InventoryClickEvent> listener = hasPermission
                ? event -> {
            if (event.isLeftClick()) {
                new MemberManageMenu(player, target, clan, filesManager).open();
            }
        }
                : null;

        return Map.entry(slot, buttonConfig.toButton(listener));
    }

    private record MembersConfig(
            String title,
            int size,
            String display,
            String rank,
            String online,
            String onlineStatus,
            String offlineStatus,
            Material fillerMaterial,
            List<Integer> fillerSlots
    ) {
        public MembersConfig {
            if (title == null || display == null || rank == null || online == null ||
                    onlineStatus == null || offlineStatus == null || fillerMaterial == null || fillerSlots == null) {
                throw new IllegalArgumentException("Required configuration values are missing in members_menu.yml");
            }
        }
    }

    private record ButtonConfig(
            int slot,
            Material material,
            String display,
            String rankLore,
            String onlineLore
    ) {
        public ButtonConfig {
            if (material == null || display == null || rankLore == null || onlineLore == null) {
                throw new IllegalArgumentException("Required button configuration values are missing");
            }
        }

        public Button toButton(Consumer<InventoryClickEvent> listener) {
            Button button = new Button(material)
                    .disableInteract(true)
                    .setDisplay(Plugin.getWithColor().hexToMinecraftColor(display))
                    .setLore(
                            Plugin.getWithColor().hexToMinecraftColor(rankLore),
                            Plugin.getWithColor().hexToMinecraftColor(onlineLore)
                    )
                    .applyMeta(meta -> {
                        if (meta instanceof SkullMeta skullMeta) {
                            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(display.split(" ")[1]));
                        }
                        return meta;
                    });

            if (listener != null) {
                button.withListener((ButtonListener) listener);
            }

            return button;
        }
    }
}
