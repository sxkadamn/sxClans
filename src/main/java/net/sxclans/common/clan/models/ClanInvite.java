package net.sxclans.common.clan.models;

import net.sxclans.common.clan.Clan;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public record ClanInvite(Player sender, Player target, Clan clan, UUID id, long timestamp, InviteStatus status) {
    public enum InviteStatus {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }

    public ClanInvite(Player sender, Player target, Clan clan) {
        this(sender, target, clan, UUID.randomUUID(), System.currentTimeMillis(), InviteStatus.PENDING);
    }

    public ClanInvite {
        Objects.requireNonNull(sender, "Sender cannot be null");
        Objects.requireNonNull(target, "Target cannot be null");
        Objects.requireNonNull(clan, "Clan cannot be null");
    }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - timestamp > timeoutMillis;
    }

    public void updateStatus(InviteStatus newStatus) {
        new ClanInvite(sender, target, clan, id, timestamp, newStatus);
    }
}