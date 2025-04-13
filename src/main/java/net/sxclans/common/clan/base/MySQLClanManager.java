package net.sxclans.common.clan.base;

import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MySQLClanManager implements ClanBaseManager {
    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private final Map<String, String> playerToClan = new ConcurrentHashMap<>();

    private final Connection connection;

    public MySQLClanManager(Connection connection) {
        this.connection = connection;
        createTables();
        loadClans();
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clans (
                    name VARCHAR(50) PRIMARY KEY,
                    leader VARCHAR(36),
                    pvp_enabled BOOLEAN,
                    member_limit INT,
                    bank DOUBLE,
                    rubles DOUBLE,
                    level INT,
                    base_world VARCHAR(64),
                    base_x DOUBLE,
                    base_y DOUBLE,
                    base_z DOUBLE,
                    base_yaw FLOAT,
                    base_pitch FLOAT
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clan_members (
                    clan_name VARCHAR(50),
                    player_name VARCHAR(36),
                    rank VARCHAR(20),
                    PRIMARY KEY (clan_name, player_name),
                    FOREIGN KEY (clan_name) REFERENCES clans(name) ON DELETE CASCADE
                )
            """);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadClans() {
        clans.clear();
        playerToClan.clear();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM clans")) {

            while (rs.next()) {
                String name = rs.getString("name");
                String leader = rs.getString("leader");

                Clan clan = new Clan(name, leader);
                clan.setPvpEnabled(rs.getBoolean("pvp_enabled"));
                clan.setMemberLimit(rs.getInt("member_limit"));
                clan.addMoneyToBank(rs.getDouble("bank") - clan.getBank());
                clan.addRubles(rs.getDouble("rubles") - clan.getRubles());
                clan.addLevel(rs.getInt("level") - clan.getLevel());

                String worldName = rs.getString("base_world");
                if (worldName != null) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location loc = new Location(
                                world,
                                rs.getDouble("base_x"),
                                rs.getDouble("base_y"),
                                rs.getDouble("base_z"),
                                rs.getFloat("base_yaw"),
                                rs.getFloat("base_pitch")
                        );
                        clan.setBaseLocation(loc);
                    }
                }

                clans.put(name, clan);
            }

            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM clan_members")) {
                try (ResultSet members = ps.executeQuery()) {
                    while (members.next()) {
                        String clanName = members.getString("clan_name");
                        String player = members.getString("player_name");
                        String rank = members.getString("rank");

                        Clan clan = clans.get(clanName);
                        if (clan != null) {
                            clan.getMembers().add(player);
                            clan.getMemberRanks().put(player, ClanRank.valueOf(rank));
                            playerToClan.put(player, clanName);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveClans() {
        for (Clan clan : clans.values()) {
            saveClan(clan);
        }
    }

    @Override
    public void saveClan(Clan clan) {
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement("""
                REPLACE INTO clans
                (name, leader, pvp_enabled, member_limit, bank, rubles, level,
                 base_world, base_x, base_y, base_z, base_yaw, base_pitch)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """)) {
                ps.setString(1, clan.getName());
                ps.setString(2, clan.getLeader());
                ps.setBoolean(3, clan.isPvpEnabled());
                ps.setInt(4, clan.getMemberLimit());
                ps.setDouble(5, clan.getBank());
                ps.setDouble(6, clan.getRubles());
                ps.setDouble(7, clan.getLevel());

                Location loc = clan.getBaseLocation();
                if (loc != null && loc.getWorld() != null) {
                    ps.setString(8, loc.getWorld().getName());
                    ps.setDouble(9, loc.getX());
                    ps.setDouble(10, loc.getY());
                    ps.setDouble(11, loc.getZ());
                    ps.setFloat(12, loc.getYaw());
                    ps.setFloat(13, loc.getPitch());
                } else {
                    for (int i = 8; i <= 13; i++) ps.setNull(i, Types.NULL);
                }

                ps.executeUpdate();
            }

            try (PreparedStatement del = connection.prepareStatement("DELETE FROM clan_members WHERE clan_name = ?")) {
                del.setString(1, clan.getName());
                del.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO clan_members (clan_name, player_name, rank)
                VALUES (?, ?, ?)
            """)) {
                for (String member : clan.getMembers()) {
                    ps.setString(1, clan.getName());
                    ps.setString(2, member);
                    ps.setString(3, clan.getRank(member).name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {}
        }

        clans.put(clan.getName(), clan);
        clan.getMembers().forEach(m -> playerToClan.put(m, clan.getName()));
    }

    @Override
    public void removeClan(Clan clan) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM clans WHERE name = ?")) {
            ps.setString(1, clan.getName());
            ps.executeUpdate();
            clans.remove(clan.getName());
            clan.getMembers().forEach(playerToClan::remove);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Clan> getClans() {
        return new HashMap<>(clans);
    }

    @Override
    public Clan getClan(String name) {
        return clans.get(name);
    }

    @Override
    public Clan getOwnersClan(String leaderName) {
        return clans.values().stream()
                .filter(clan -> clan.getLeader().equals(leaderName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Clan getMembersClan(String memberName) {
        String name = playerToClan.get(memberName);
        return name == null ? null : clans.get(name);
    }

    @Override
    public void addWarRequest(String senderClanName, String receiverClanName) {

    }

    @Override
    public void acceptWarRequest(String receiverClanName) {

    }

    @Override
    public void cancelWarRequest(String receiverClanName) {

    }

    @Override
    public Map<String, String> getWarRequests() {
        return Map.of();
    }
}