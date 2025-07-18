package net.sxclans.common.clan.menus.management;

import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.lielibrary.gui.buttons.ButtonListener;
import net.sxclans.bukkit.Depend;
import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.util.Consumer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;


public class MemberManageMenu {
    private final Player moderator;
    private final OfflinePlayer target;
    private final Clan clan;
    private final AnimatedMenu menu;
    private final MemberManageConfig config;

    public MemberManageMenu(Player moderator, OfflinePlayer target, Clan clan, FilesManagerInterface filesManager) {
        this.moderator = moderator;
        this.target = target;
        this.clan = clan;
        FileConfiguration fileConfig = filesManager.getMenuConfig("member_manage");
        this.config = new MemberManageConfig(
                fileConfig.getString("title"),
                fileConfig.getInt("size"),
                fileConfig.getString("messages.kick_success"),
                fileConfig.getString("messages.kicked"),
                fileConfig.getString("messages.promote_success"),
                fileConfig.getString("messages.promoted"),
                fileConfig.getString("messages.max_rank"),
                fileConfig.getInt("kick_slot"),
                fileConfig.getInt("promote_slot"),
                new ButtonConfig(
                        Material.BARRIER,
                        fileConfig.getString("kick_button"),
                        fileConfig.getString("kick_lore")
                ),
                new ButtonConfig(
                        Material.EMERALD,
                        fileConfig.getString("promote_button"),
                        fileConfig.getString("promote_lore")
                ),
                Material.valueOf(fileConfig.getString("filler.material")),
                fileConfig.getIntegerList("filler.slots"),

                fileConfig.getInt("demote_slot"),
                new ButtonConfig(
                        Material.REDSTONE,
                        fileConfig.getString("demote_button"),
                        fileConfig.getString("demote_lore")
                ),
                fileConfig.getString("messages.demote_success"),
                fileConfig.getString("messages.demoted"),
                fileConfig.getString("messages.min_rank")
        );

        this.menu = Plugin.getMenuManager().createMenuFromConfig(
                Plugin.getWithColor().hexToMinecraftColor(config.title().replace("{player}", target.getName())),
                config.size(),
                moderator
        );
    }

    public void open() {
        if (menu == null || config == null) return;

        ClanRank currentRank = clan.getRank(target.getName());

        for (int slot : config.fillerSlots()) {
            if (slot >= 0 && slot < config.size() * 9) {
                menu.setSlot(slot, new Button(config.fillerMaterial())
                        .setDisplay(" ")
                        .disableInteract(true)
                        .applyMeta(meta -> meta));
            }
        }

        menu.setSlot(
                config.demoteSlot(),
                config.demoteButton().withSlot(config.demoteSlot()).toButton(event -> {
                    ClanRank previousRank = currentRank.getPreviousRank();
                    Optional.ofNullable(previousRank)
                            .ifPresentOrElse(
                                    rank -> {
                                        clan.setRank(target.getName(), rank);
                                        moderator.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                                config.demoteSuccess()
                                                        .replace("{player}", target.getName())
                                                        .replace("{rank}", rank.getDisplayName())));
                                        Optional.ofNullable(target.getPlayer())
                                                .filter(Player::isOnline)
                                                .ifPresent(player -> player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                                        config.demoted().replace("{rank}", rank.getDisplayName()))));
                                        clan.save();
                                    },
                                    () -> moderator.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.minRank()))
                            );
                    moderator.closeInventory();
                })
        );

        menu.setSlot(
                config.kickSlot(),
                config.kickButton().withSlot(config.kickSlot()).toButton(event -> {
                    Depend.getClanManage().removeMember(clan, target.getName());
                    clan.removeMember(target.getName());
                    clan.getMemberRanks().remove(target.getName());
                    moderator.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                            config.kickSuccess().replace("{player}", target.getName())));
                    Optional.ofNullable(target.getPlayer())
                            .filter(Player::isOnline)
                            .ifPresent(player -> player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                    config.kicked())));
                    clan.save();
                    moderator.closeInventory();
                })
        );

        menu.setSlot(
                config.promoteSlot(),
                config.promoteButton().withSlot(config.promoteSlot()).toButton(event -> {
                    ClanRank nextRank = currentRank.getNextRank();
                    Optional.ofNullable(nextRank)
                            .ifPresentOrElse(
                                    rank -> {
                                        clan.setRank(target.getName(), rank);
                                        moderator.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                                config.promoteSuccess()
                                                        .replace("{player}", target.getName())
                                                        .replace("{rank}", rank.getDisplayName())));
                                        Optional.ofNullable(target.getPlayer())
                                                .filter(Player::isOnline)
                                                .ifPresent(player -> player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                                        config.promoted().replace("{rank}", rank.getDisplayName()))));
                                        clan.save();
                                    },
                                    () -> moderator.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.maxRank()))
                            );
                    moderator.closeInventory();
                })
        );

        menu.open(moderator);
    }

    private record ButtonConfig(
            int slot,
            Material material,
            String display,
            String lore
    ) {
        public ButtonConfig(Material material, String display, String lore) {
            this(-1, material, display, lore);
        }

        public ButtonConfig {
            if (material == null || display == null) {
                throw new IllegalArgumentException("Required button configuration values are missing");
            }
        }

        public Button toButton(ButtonListener listener) {
            return new Button(material)
                    .setDisplay(Plugin.getWithColor().hexToMinecraftColor(display))
                    .setLore(Plugin.getWithColor().hexToMinecraftColor(lore))
                    .withListener(listener);
        }

        public ButtonConfig withSlot(int slot) {
            return new ButtonConfig(slot, material, display, lore);
        }
    }

    private record MemberManageConfig(
            String title,
            int size,
            String kickSuccess,
            String kicked,
            String promoteSuccess,
            String promoted,
            String maxRank,
            int kickSlot,
            int promoteSlot,
            ButtonConfig kickButton,
            ButtonConfig promoteButton,
            Material fillerMaterial,
            List<Integer> fillerSlots,
            int demoteSlot,
            ButtonConfig demoteButton,
            String demoteSuccess,
            String demoted,
            String minRank
    ) {
        public MemberManageConfig {
            if (title == null || kickSuccess == null || kicked == null ||
                    promoteSuccess == null || promoted == null || maxRank == null ||
                    kickButton == null || promoteButton == null || fillerMaterial == null || fillerSlots == null) {
                throw new IllegalArgumentException("Required configuration values are missing in member_manage.yml");
            }
        }
    }
}
