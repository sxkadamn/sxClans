package net.sxclans.common.clan;

import net.sxclans.bukkit.Depend;

import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.*;
import java.util.*;

public class Clan implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String leader;
    private final List<String> members;
    private final Map<String, ClanRank> memberRanks;
    private boolean pvpEnabled;
    private transient Location baseLocation;
    private int memberLimit;
    private double bank;
    private double rubles;
    private double level;

    public Clan(String name, String owner) {
        this.name = name;
        this.leader = owner;
        this.level = 1;
        this.rubles = 0;
        this.members = new ArrayList<>();
        this.memberRanks = new HashMap<>();
        this.pvpEnabled = false;
        this.baseLocation = null;
        this.memberLimit = Depend.getInstance().getConfig().getInt("clan_settings.default_member_limit", 10);
        this.bank = 0.0;

        members.add(owner);
        memberRanks.put(owner, ClanRank.LEADER);
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (baseLocation != null) {
            out.writeBoolean(true);
            out.writeUTF(baseLocation.getWorld().getName());
            out.writeDouble(baseLocation.getX());
            out.writeDouble(baseLocation.getY());
            out.writeDouble(baseLocation.getZ());
            out.writeFloat(baseLocation.getYaw());
            out.writeFloat(baseLocation.getPitch());
        } else {
            out.writeBoolean(false);
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (in.readBoolean()) {
            String worldName = in.readUTF();
            double x = in.readDouble();
            double y = in.readDouble();
            double z = in.readDouble();
            float yaw = in.readFloat();
            float pitch = in.readFloat();
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                baseLocation = new Location(world, x, y, z, yaw, pitch);
            } else {
                baseLocation = null;
            }
        } else {
            baseLocation = null;
        }
    }

    public void save() {
        Depend.getClanManage().saveClan(this);
    }

    public List<String> getMembers() {
        return members;
    }

    public void addMember(String playerName) {
        if (members.size() < memberLimit && !members.contains(playerName)) {
            members.add(playerName);
            Depend.getClanManage().saveClan(this);
        }
    }

    public void removeMember(String playerName) {
        members.remove(playerName);
        memberRanks.remove(playerName);
        save();
        Depend.getClanManage().saveClan(this);
    }

    public void setRank(String playerName, ClanRank rank) {
        memberRanks.put(playerName, rank);
    }

    public ClanRank getRank(String playerName) {
        return memberRanks.getOrDefault(playerName, ClanRank.MEMBER);
    }


    public Map<String, ClanRank> getMemberRanks() {
        return memberRanks;
    }

    public boolean hasPermission(String playerName, ClanRank requiredRank) {
        ClanRank playerRank = getMemberRank(playerName);
        if (playerRank == null) return false;
        return playerRank.ordinal() <= requiredRank.ordinal();
    }

    public ClanRank getMemberRank(String playerName) {
        return memberRanks.getOrDefault(playerName, ClanRank.MEMBER);
    }

    public long countMembersByRank(ClanRank rank) {
        return memberRanks.values().stream().filter(r -> r == rank).count();
    }

    public String getLeader() {
        return this.leader;
    }

    public void addMoneyToBank(double amount) {
        this.bank += amount;
        save();
    }

    public void withdrawMoneyFromBank(double amount) {
        if (this.bank >= amount) {
            this.bank -= amount;
            save();
        }
    }

    public double getBank() {
        return bank;
    }

    public void addRubles(double amount) {
        this.rubles += amount;
        save();
    }

    public void addLevel(double amount) {
        this.level += amount;
        save();
    }

    public double getLevel() {
        return this.level;
    }

    public double getRubles() {
        return this.rubles;
    }

    public String getName() {
        return this.name;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean enabled) {
        this.pvpEnabled = enabled;
        save();
    }

    public Location getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(Location location) {
        this.baseLocation = location;
        save();
    }

    public int getMemberLimit() {
        return memberLimit;
    }

    public void setMemberLimit(int limit) {
        this.memberLimit = limit;
        save();
    }
}
