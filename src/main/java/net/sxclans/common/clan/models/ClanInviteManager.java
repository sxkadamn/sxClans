package net.sxclans.common.clan.models;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.lielibrary.bukkit.Plugin;
import net.sxclans.bukkit.Depend;
import net.sxclans.common.Utility;
import net.sxclans.common.clan.Clan;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class ClanInviteManager {
    private static final Map<UUID, ClanInvite> pendingInvites = new HashMap<>();
    private static BukkitAudiences adventure;
    private static ConfigurationSection config;
    private static final LegacyComponentSerializer COLOR_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .build();
    private static final Set<UUID> recentlyInvited = new HashSet<>();


    public static void initialize() {
        adventure = BukkitAudiences.create(Depend.getInstance());
        config = Depend.getInstance().getConfig().getConfigurationSection("invite");
    }

    public static boolean hasPendingInvite(UUID playerId) {
        if (config == null) return false;
        ClanInvite invite = pendingInvites.get(playerId);
        if (invite == null) return false;
        if (invite.isExpired(config.getLong("timeout") * 20L)) {
            pendingInvites.remove(playerId);
            return false;
        }
        return invite.status() == ClanInvite.InviteStatus.REJECTED;
    }

    public static void sendClanInvite(Player sender, Player target, Clan clan) {
        if (config == null || adventure == null) return;

        UUID targetId = target.getUniqueId();
        long timeoutTicks = config.getLong("timeout") * 20L;

        if (hasPendingInvite(targetId)) {
            sender.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                    config.getString("errors.already_invited")));
            return;
        }

        if (recentlyInvited.contains(targetId)) return;
        recentlyInvited.add(targetId);
        Bukkit.getScheduler().runTaskLater(Depend.getInstance(), () -> recentlyInvited.remove(targetId), 2L);

        ClanInvite newInvite = new ClanInvite(sender, target, clan);
        pendingInvites.put(targetId, newInvite);

        String acceptText = config.getString("buttons.accept");
        String declineText = config.getString("buttons.deny");

        TextComponent.Builder builder = Component.text();

        builder.append(Component.text("\n"));
        builder.append(COLOR_SERIALIZER.deserialize(
                Plugin.getWithColor().hexToMinecraftColor(
                        config.getString("invite_message")
                                .replace("{sender}", sender.getName())
                                .replace("{clan}", clan.getName())
                )
        ));
        builder.append(Component.text("\n"));

        for (String line : config.getStringList("message_request")) {
            if (line == null || line.isEmpty()) {
                builder.append(Component.text("\n"));
                continue;
            }

            if (line.contains("{accept}") && line.contains("{decline}")) {
                String[] parts = line.split("\\{accept}|\\{decline}");
                if (parts.length >= 2) {
                    builder.append(COLOR_SERIALIZER.deserialize(Plugin.getWithColor().hexToMinecraftColor(parts[0])))
                            .append(COLOR_SERIALIZER.deserialize(Plugin.getWithColor().hexToMinecraftColor(acceptText))
                                    .clickEvent(ClickEvent.runCommand("/clan accept")))
                            .append(COLOR_SERIALIZER.deserialize(Plugin.getWithColor().hexToMinecraftColor(parts[1])))
                            .append(COLOR_SERIALIZER.deserialize(Plugin.getWithColor().hexToMinecraftColor(declineText))
                                    .clickEvent(ClickEvent.runCommand("/clan deny")));

                    if (parts.length > 2) {
                        builder.append(COLOR_SERIALIZER.deserialize(Plugin.getWithColor().hexToMinecraftColor(parts[2])));
                    }
                } else {
                    builder.append(COLOR_SERIALIZER.deserialize(Plugin.getWithColor().hexToMinecraftColor(line)));
                }
            } else {
                builder.append(COLOR_SERIALIZER.deserialize(
                        Plugin.getWithColor().hexToMinecraftColor(
                                line.replace("{clan}", clan.getName())
                                        .replace("{accept}", acceptText)
                                        .replace("{decline}", declineText)
                        )
                ));
            }

            builder.append(Component.text("\n"));
        }

        adventure.player(target).sendMessage(builder.build());
        sender.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                config.getString("sent_message").replace("{target}", target.getName())));

        Bukkit.getScheduler().runTaskLater(Depend.getInstance(), () -> {
            ClanInvite invite = pendingInvites.get(targetId);
            if (invite != null && invite.status() == ClanInvite.InviteStatus.PENDING) {
                pendingInvites.remove(targetId);
                invite.updateStatus(ClanInvite.InviteStatus.EXPIRED);

                if (sender.isOnline()) {
                    sender.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                            config.getString(".errors.expired_sender").replace("{target}", target.getName())));
                }

                if (target.isOnline()) {
                    target.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                            config.getString(".errors.expired_target")));
                }
            }
        }, timeoutTicks);
    }

    public static void acceptInvite(Player player) {
        if (config == null) return;

        UUID playerId = player.getUniqueId();
        long timeoutMillis = config.getLong("timeout") * 50;
        ClanInvite invite = pendingInvites.get(playerId);

        if (invite == null) {
            sendError(player, ".errors.no_pending_invites");
            return;
        }

        if (invite.isExpired(timeoutMillis)) {
            pendingInvites.remove(playerId);
            sendError(player, ".errors.expired_target");
            return;
        }

        if (invite.status() != ClanInvite.InviteStatus.PENDING) {
            sendError(player, ".errors.invalid_invite_status");
            return;
        }

        if (invite.clan().getMembers().size() >= invite.clan().getMemberLimit()) {
            sendError(player, ".errors.max_limit_reached");
            return;
        }

        pendingInvites.remove(playerId);
        invite.updateStatus(ClanInvite.InviteStatus.ACCEPTED);
        invite.clan().addMember(player.getName());
        invite.clan().setRank(player.getName(), ClanRank.MEMBER);
        Utility.playConfiguredSounds(player, "sounds.clan_joined");

        String acceptedMsg = config.getString(".errors.accepted");
        if (acceptedMsg != null)
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                    acceptedMsg.replace("{clan}", invite.clan().getName())));

        if (invite.sender().isOnline()) {
            String senderMsg = config.getString(".errors.accepted_sender");
            if (senderMsg != null)
                invite.sender().sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                        senderMsg.replace("{player}", player.getName())));
        }
    }

    public static void denyInvite(Player player) {
        if (config == null) return;

        UUID playerId = player.getUniqueId();
        ClanInvite invite = pendingInvites.remove(playerId);

        if (invite == null || invite.status() != ClanInvite.InviteStatus.PENDING) {
            sendError(player, ".errors.no_pending_invites");
            return;
        }

        invite.updateStatus(ClanInvite.InviteStatus.REJECTED);

        String deniedMsg = config.getString(".errors.denied");
        if (deniedMsg != null)
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                    deniedMsg.replace("{clan}", invite.clan().getName())));

        if (invite.sender().isOnline()) {
            String deniedSenderMsg = config.getString(".errors.denied_sender");
            if (deniedSenderMsg != null)
                invite.sender().sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                        deniedSenderMsg.replace("{player}", player.getName())));
        }
    }


    public static void cleanup() {
        pendingInvites.clear();
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
        config = null;
    }

    private static void sendError(Player player, String path) {
        String msg = config.getString(path);
        if (msg != null)
            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor(msg));
    }
}
