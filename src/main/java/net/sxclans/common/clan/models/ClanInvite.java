package net.sxclans.common.clan.models;

import net.sxclans.common.clan.Clan;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public final class ClanInvite {
    public enum InviteStatus {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }

    private final Player sender;
    private final Player target;
    private final Clan clan;
    private final UUID id;
    private final long timestamp;
    private InviteStatus status;

    public ClanInvite(Player sender, Player target, Clan clan) {
        this(sender, target, clan, UUID.randomUUID(), System.currentTimeMillis(), InviteStatus.PENDING);
    }

    public ClanInvite(Player sender, Player target, Clan clan, UUID id, long timestamp, InviteStatus status) {
        this.sender = Objects.requireNonNull(sender, "Sender cannot be null");
        this.target = Objects.requireNonNull(target, "Target cannot be null");
        this.clan = Objects.requireNonNull(clan, "Clan cannot be null");
        this.id = id;
        this.timestamp = timestamp;
        this.status = status;
    }

    public Player sender() { return sender; }
    public Player target() { return target; }
    public Clan clan() { return clan; }
    public UUID id() { return id; }
    public long timestamp() { return timestamp; }
    public InviteStatus status() { return status; }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - timestamp > timeoutMillis;
    }

    public void updateStatus(InviteStatus newStatus) {
        this.status = newStatus;
    }
}