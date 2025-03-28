package net.sxclans.common.clan.models;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.lielibrary.bukkit.Plugin;
import net.sxclans.bukkit.Depend;
import net.sxclans.common.Utility;
import net.sxclans.common.clan.Clan;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClanInviteManager {
    private static final Map<UUID, ClanInvite> pendingInvites = new HashMap<>();
    private static final BukkitAudiences adventure = BukkitAudiences.create(Depend.getInstance());
    private static final ConfigurationSection config = Depend.getInstance().getConfig().getConfigurationSection("invite");

    public static void sendClanInvite(Player sender, Player target, Clan clan) {
        UUID targetId = target.getUniqueId();
        long timeoutTicks = config.getLong("timeout") * 50;

        Optional.ofNullable(pendingInvites.get(targetId))
                .filter(invite -> invite.status() == ClanInvite.InviteStatus.PENDING && !invite.isExpired(timeoutTicks))
                .ifPresent(invite -> {
                    sender.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("errors.already_invited")));
                    return;
                });

        pendingInvites.computeIfPresent(targetId, (id, invite) -> {
            invite.updateStatus(ClanInvite.InviteStatus.EXPIRED);
            return null;
        });

        ClanInvite newInvite = new ClanInvite(sender, target, clan);
        newInvite.updateStatus(ClanInvite.InviteStatus.PENDING);
        pendingInvites.put(targetId, newInvite);

        Audience audience = adventure.player(target);
        audience.sendMessage(
                Component.text(Plugin.getWithColor().hexToMinecraftColor(
                        config.getString("invite_message")
                                .replace("{sender}", sender.getName())
                                .replace("{clan}", clan.getName())))
        );
        audience.sendMessage(
                Component.text(Plugin.getWithColor().hexToMinecraftColor(config.getString("buttons.accept")))
                        .clickEvent(ClickEvent.runCommand("/clan accept"))
                        .append(Component.text(" "))
                        .append(Component.text(Plugin.getWithColor().hexToMinecraftColor(config.getString("buttons.deny")))
                                .clickEvent(ClickEvent.runCommand("/clan deny")))
        );

        sender.closeInventory();
        sender.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                config.getString("sent_message").replace("{target}", target.getName())));

        Bukkit.getScheduler().runTaskLater(Depend.getInstance(), () -> {
            Optional.ofNullable(pendingInvites.get(targetId))
                    .filter(invite -> invite.status() == ClanInvite.InviteStatus.PENDING)
                    .ifPresent(invite -> {
                        invite.updateStatus(ClanInvite.InviteStatus.EXPIRED);
                        pendingInvites.remove(targetId);
                        if (sender.isOnline()) sender.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                config.getString("errors.expired_sender").replace("{target}", target.getName())));
                        if (target.isOnline()) target.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                config.getString("errors.expired_target")));
                    });
        }, timeoutTicks);
    }

    public static void acceptInvite(Player player) {
        if (config == null) return;

        UUID playerId = player.getUniqueId();
        long timeoutTicks = config.getLong("invite.timeout", 1200) * 50;

        Optional.ofNullable(pendingInvites.get(playerId))
                .map(invite -> {
                    if (invite.isExpired(timeoutTicks)) {
                        invite.updateStatus(ClanInvite.InviteStatus.EXPIRED);
                        pendingInvites.remove(playerId);
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                config.getString("invite.errors.expired_target")
                        ));
                        return null;
                    }
                    return invite;
                })
                .filter(invite -> invite.status() == ClanInvite.InviteStatus.PENDING)
                .filter(invite -> invite.clan().getMembers().size() < invite.clan().getMemberLimit())
                .ifPresentOrElse(invite -> {
                    invite.updateStatus(ClanInvite.InviteStatus.ACCEPTED);
                    pendingInvites.remove(playerId);
                    invite.clan().addMember(player.getName());
                    invite.clan().setRank(player.getName(), ClanRank.MEMBER);
                    Utility.playConfiguredSounds(player, "sounds.clan_joined");
                    String acceptMessage = config.getString("errors.accepted");
                    player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                            acceptMessage.replace("{clan}", invite.clan().getName())
                    ));
                    if (invite.sender().isOnline()) {
                        String senderMessage = config.getString("errors.accepted_sender");
                        invite.sender().sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                                senderMessage.replace("{player}", player.getName())
                        ));
                    }
                }, () -> player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                        config.getString("errors.max_limit_reached")
                )));
    }

    public static void denyInvite(Player player) {
        UUID playerId = player.getUniqueId();

        Optional.ofNullable(pendingInvites.remove(playerId))
                .filter(invite -> invite.status() == ClanInvite.InviteStatus.PENDING)
                .ifPresent(invite -> {
                    invite.updateStatus(ClanInvite.InviteStatus.REJECTED);
                    player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(config.getString("errors.denied")));
                    if (invite.sender().isOnline()) invite.sender().sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                            config.getString("errors.denied_sender").replace("{player}", player.getName())));
                });
    }
}
