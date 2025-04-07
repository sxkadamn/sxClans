package net.sxclans.common.clan.menus.interactions;


import net.lielibrary.AnimatedMenu;
import net.lielibrary.bukkit.Plugin;
import net.lielibrary.gui.buttons.Button;
import net.sxclans.bukkit.Depend;

import net.sxclans.bukkit.files.FilesManagerInterface;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.models.ClanInviteManager;
import org.bukkit.Bukkit;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import org.bukkit.inventory.meta.SkullMeta;


import java.util.Optional;



public class InviteMenu {

    private final Player player;
    private final Clan clan;
    private final AnimatedMenu menu;
    private final InviteConfig config;

    public InviteMenu(Player player, FilesManagerInterface filesManager) {
        this.player = player;
        this.clan = Depend.getClanManage().getMembersClan(player.getName());
        FileConfiguration fileConfig = filesManager.getMenuConfig("members");
        this.config = new InviteConfig(
                fileConfig.getString("name"),
                fileConfig.getInt("size"),
                fileConfig.getInt("max_slots"),
                fileConfig.getString("invite_display")
        );
        this.menu = Optional.of(config)
                .map(cfg -> Plugin.getMenuManager().createMenuFromConfig(
                        Plugin.getWithColor().hexToMinecraftColor(cfg.name()),
                        cfg.size(), player))
                .orElse(null);
    }

    public void open() {
        if (menu == null || config == null) return;

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> Depend.getClanManage().getMembersClan(p.getName()) == null)
                .limit(config.maxSlots())
                .forEach(target -> {
                    int slot = 0;
                    while (menu.getSlot(slot) != null && slot < config.size()) {
                        slot++;
                    }

                    if (slot >= config.size()) return;

                    String displayText = Plugin.getWithColor().hexToMinecraftColor(
                            config.inviteDisplay().replace("{player}", target.getName()));

                    menu.setSlot(slot, new Button(Material.PLAYER_HEAD)
                            .setDisplay(displayText)
                            .withListener(event -> ClanInviteManager.sendClanInvite(player, target, clan))
                            .applyMeta(meta -> {
                                if (meta instanceof SkullMeta skullMeta) {
                                    skullMeta.setOwningPlayer(target);
                                }
                                return meta;
                            }));
                });

        menu.open(player);
    }

    private record InviteConfig(
            String name,
            int size,
            int maxSlots,
            String inviteDisplay
    ) {
        public InviteConfig {
            if (name == null || inviteDisplay == null) {
                throw new IllegalArgumentException("Required configuration values are missing in members.yml");
            }
        }
    }

}
