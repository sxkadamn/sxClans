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

    private List<String> warParticipants;
    private boolean isInWar;
    private String enemyClanName;
    private int warScore;

    public Clan(String name, String owner) {
        this.name = name;
        this.leader = owner;
        this.level = 0.0;
        this.rubles = 0.0;
        this.members = new ArrayList<>();
        this.memberRanks = new HashMap<>();
        this.pvpEnabled = false;
        this.baseLocation = null;
        this.memberLimit = Depend.getInstance().getConfig().getInt("clan_settings.default_member_limit", 10);
        this.bank = 0.0;

        members.add(owner);
        memberRanks.put(owner, ClanRank.LEADER);

        this.warParticipants = new ArrayList<>();
        this.isInWar = false;
        this.enemyClanName = null;
        this.warScore = 0;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeBoolean(baseLocation != null);
        if (baseLocation != null) {
            out.writeUTF(baseLocation.getWorld().getName());
            out.writeDouble(baseLocation.getX());
            out.writeDouble(baseLocation.getY());
            out.writeDouble(baseLocation.getZ());
            out.writeFloat(baseLocation.getYaw());
            out.writeFloat(baseLocation.getPitch());
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (in.readBoolean()) {
            String worldName = in.readUTF();
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                baseLocation = new Location(
                        world,
                        in.readDouble(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readFloat(),
                        in.readFloat()
                );
            }
        }

        if (warParticipants == null) {
            warParticipants = new ArrayList<>();
        }
    }

    public void save() {
        Bukkit.getScheduler().runTaskAsynchronously(Depend.getInstance(), () -> Depend.getClanManage().saveClan(this));
    }

    public List<String> getMembers() {
        return members;
    }

    public void addMember(String playerName) {
        if (members.size() >= memberLimit || members.contains(playerName)) return;
        members.add(playerName);
        memberRanks.put(playerName, ClanRank.MEMBER);
        save();
    }

    public void removeMember(String playerName) {
        members.remove(playerName);
        memberRanks.remove(playerName);
        save();
    }

    public String getMember(String playerName) {
        for (String member : members) {
            if (member.equalsIgnoreCase(playerName)) {
                return member;
            }
        }
        return null;
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
        return playerRank != null && playerRank.getPower() >= requiredRank.getPower();
    }

    public ClanRank getMemberRank(String name) {
        return memberRanks.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
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

    public void setBank(double bank) {
        this.bank = bank;
    }

    public void setRubles(double rubles) {
        this.rubles = rubles;
    }

    public double getRubles() {
        return this.rubles;
    }

    public void addRubles(double amount) {
        this.rubles += amount;
        save();
    }

    public int getLevel() {
        return (int) level;
    }

    public double getExactLevel() {
        return level;
    }

    public void setLevel(double level) {
        this.level = level;
        save();
    }

    public void addLevel(double amount) {
        this.level += amount;
        save();
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
        if (location == null || location.getWorld() == null)
            throw new IllegalArgumentException("Локация не может быть null!");
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

    public String getStrippedName() {
        return name.replaceAll("(?i)[§&][0-9A-FK-ORX]", "");
    }

    public void sendWarRequest(Clan enemyClan) {
        if (this.isInWar || enemyClan.isInWar) {
            Depend.getInstance().getLogger().info("Один из кланов уже участвует в войне.");
            return;
        }
        Depend.getClanManage().addWarRequest(this.name, enemyClan.getName());
    }

    public void acceptWarRequest(Clan enemyClan) {
        if (this.isInWar || enemyClan.isInWar) {
            Depend.getInstance().getLogger().info("Один из кланов уже участвует в войне.");
            return;
        }
        this.isInWar = true;
        enemyClan.setInWar(true);
        this.enemyClanName = enemyClan.getName();
        enemyClan.setEnemyClanName(this.name);
    }

    public void endWar() {
        this.isInWar = false;
        this.enemyClanName = null;
        this.warScore = 0;
        this.warParticipants.clear();
    }

    public boolean isInWar() {
        return isInWar;
    }

    public void setInWar(boolean inWar) {
        isInWar = inWar;
    }

    public String getEnemyClanName() {
        return enemyClanName;
    }

    public void setEnemyClanName(String enemyClanName) {
        this.enemyClanName = enemyClanName;
    }

    public int getWarScore() {
        return warScore;
    }

    public void setWarScore(int warScore) {
        this.warScore = warScore;
    }

    public void addWarScore(int score) {
        this.warScore += score;
    }

    public List<String> getWarParticipants() {
        return warParticipants;
    }

    public void setWarParticipants(List<String> warParticipants) {
        this.warParticipants = warParticipants;
    }

    public boolean isWarParticipant(String playerName) {
        return warParticipants.contains(playerName);
    }

    public void addWarParticipant(String name) {
        if (!warParticipants.contains(name)) {
            warParticipants.add(name);
        }
    }

    public void removeWarParticipant(String playerName) {
        warParticipants.remove(playerName);
    }
}
