package net.sxclans.common.clan.base;

import net.sxclans.bukkit.Depend;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.models.ClanRank;
import net.sxclans.common.clan.war.arena.Arena;
import net.sxclans.common.clan.war.manager.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MySQLClanManager implements ClanBaseManager {
    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private final Map<String, String> playerToClan = new ConcurrentHashMap<>();
    private final Map<String, String> warRequests = new ConcurrentHashMap<>();
    private final Map<String, WarManager> activeWars = new ConcurrentHashMap<>();
    private final Connection connection;

    public MySQLClanManager(Connection connection) {
        this.connection = connection;
        createTables();
        loadAllData();
    }

    @Override
    public void removeMember(Clan clan, String member) {
        try {
            clan.getMembers().forEach(playerToClan::remove);
        } catch (Exception e) {
        } finally {
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clans (
                    name VARCHAR(50) PRIMARY KEY,
                    leader VARCHAR(36) NOT NULL,
                    pvp_enabled BOOLEAN NOT NULL DEFAULT false,
                    member_limit INT NOT NULL DEFAULT 20,
                    bank DOUBLE NOT NULL DEFAULT 0.0,
                    rubles DOUBLE NOT NULL DEFAULT 0.0,
                    level INT NOT NULL DEFAULT 1,
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
                    clan_name VARCHAR(50) REFERENCES clans(name) ON DELETE CASCADE,
                    player_uuid VARCHAR(36) NOT NULL,
                    rank VARCHAR(20) NOT NULL,
                    PRIMARY KEY (clan_name, player_uuid)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_requests (
                    sender_clan VARCHAR(50) REFERENCES clans(name) ON DELETE CASCADE,
                    receiver_clan VARCHAR(50) REFERENCES clans(name) ON DELETE CASCADE,
                    PRIMARY KEY (sender_clan, receiver_clan)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS active_wars (
                    clan1 VARCHAR(50) REFERENCES clans(name) ON DELETE CASCADE,
                    clan2 VARCHAR(50) REFERENCES clans(name) ON DELETE CASCADE,
                    start_time BIGINT NOT NULL,
                    PRIMARY KEY (clan1, clan2)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_participants (
                    clan_name VARCHAR(50) REFERENCES clans(name) ON DELETE CASCADE,
                    player_uuid VARCHAR(36) NOT NULL,
                    PRIMARY KEY (clan_name, player_uuid)
                )
            """);

        } catch (SQLException e) {
            logError("Error creating tables", e);
        }
    }

    private void loadAllData() {
        loadClans();
        loadMembers();
        loadWarParticipants();
        loadWarRequests();
        loadActiveWars();
    }

    @Override
    public void loadClans() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM clans");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Clan clan = new Clan(
                        rs.getString("name"),
                        rs.getString("leader")
                );

                configureClanFromResultSet(clan, rs);
                clans.put(clan.getName(), clan);
            }
        } catch (SQLException e) {
            logError("Error loading clans", e);
        }
    }

    private void configureClanFromResultSet(Clan clan, ResultSet rs) throws SQLException {
        clan.setPvpEnabled(rs.getBoolean("pvp_enabled"));
        clan.setMemberLimit(rs.getInt("member_limit"));
        clan.setBank(rs.getDouble("bank"));
        clan.setRubles(rs.getDouble("rubles"));
        clan.setLevel(rs.getInt("level"));

        Location baseLoc = parseLocation(rs);
        if (baseLoc != null) clan.setBaseLocation(baseLoc);
    }

    private Location parseLocation(ResultSet rs) throws SQLException {
        String worldName = rs.getString("base_world");
        World world = worldName != null ? Bukkit.getWorld(worldName) : null;
        return world != null ? new Location(
                world,
                rs.getDouble("base_x"),
                rs.getDouble("base_y"),
                rs.getDouble("base_z"),
                rs.getFloat("base_yaw"),
                rs.getFloat("base_pitch")
        ) : null;
    }

    private void loadMembers() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM clan_members");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Clan clan = clans.get(rs.getString("clan_name"));
                if (clan != null) {
                    String uuid = rs.getString("player_uuid");
                    clan.addMember(uuid);
                    playerToClan.put(uuid, clan.getName());
                }
            }
        } catch (SQLException e) {
            logError("Error loading members", e);
        }
    }

    private void loadWarParticipants() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM war_participants");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Clan clan = clans.get(rs.getString("clan_name"));
                if (clan != null) {
                    clan.addWarParticipant(rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            logError("Error loading war participants", e);
        }
    }

    private void loadWarRequests() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM war_requests");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                warRequests.put(
                        rs.getString("receiver_clan"),
                        rs.getString("sender_clan")
                );
            }
        } catch (SQLException e) {
            logError("Error loading war requests", e);
        }
    }

    private void loadActiveWars() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM active_wars");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Clan clan1 = clans.get(rs.getString("clan1"));
                Clan clan2 = clans.get(rs.getString("clan2"));

                if (clan1 != null && clan2 != null) {

                    Optional<Arena> optionalArena = Arena.getArenas().stream()
                            .filter(Arena::isOpen)
                            .findFirst();

                    if (optionalArena.isEmpty()) {
                        System.out.println("[WAR] No free arenas available to restore war: "
                                + clan1.getName() + " vs " + clan2.getName());
                        continue;
                    }

                    Arena arena = optionalArena.get();

                    WarManager war = new WarManager(clan1, clan2, arena);
                    activeWars.put(generateWarKey(clan1, clan2), war);
                }
            }
        } catch (SQLException e) {
            logError("Error loading active wars", e);
        }
    }


    @Override
    public void addWarRequest(String sender, String receiver) {
        warRequests.put(receiver, sender);

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO war_requests VALUES (?,?) ON DUPLICATE KEY UPDATE sender_clan=VALUES(sender_clan)")) {

            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("Error adding war request", e);
        }
    }

    @Override
    public void acceptWarRequest(String receiver) {
        String sender = warRequests.remove(receiver);
        if (sender == null) return;

        Clan clan1 = clans.get(sender);
        Clan clan2 = clans.get(receiver);
        if (clan1 == null || clan2 == null) return;

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM war_requests WHERE receiver_clan = ?")) {

            ps.setString(1, receiver);
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("Error accepting war request", e);
        }

        Optional<Arena> optionalArena = Arena.getArenas().stream()
                .filter(Arena::isOpen)
                .findFirst();

        if (optionalArena.isEmpty()) {
            System.out.println("[WAR] No available arenas to start war between " + sender + " and " + receiver);
            return;
        }

        Arena arena = optionalArena.get();

        WarManager war = new WarManager(clan1, clan2, arena);
        setActiveWarManager(clan1, clan2, war);
        war.startWarCountdown();
    }


    @Override
    public void cancelWarRequest(String receiver) {
        warRequests.remove(receiver);

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM war_requests WHERE receiver_clan = ?")) {

            ps.setString(1, receiver);
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("Error canceling war request", e);
        }
    }

    @Override
    public Map<String, String> getWarRequests() {
        return new HashMap<>(warRequests);
    }

    @Override
    public Map<String, WarManager> getActiveWars() {
        return new HashMap<>(activeWars);
    }

    @Override
    public WarManager getActiveWarManager(Player player) {
        String clanName = playerToClan.get(player.getUniqueId().toString());

        return clanName != null ? activeWars.values().stream()
                .filter(war -> war.isParticipant(player))
                .findFirst()
                .orElse(null) : null;
    }

    @Override
    public void setActiveWarManager(Clan clanA, Clan clanB, WarManager war) {
        String key = generateWarKey(clanA, clanB);
        activeWars.put(key, war);

        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO active_wars VALUES (?,?,?)
            ON DUPLICATE KEY UPDATE start_time=VALUES(start_time)
        """)) {

            ps.setString(1, clanA.getName());
            ps.setString(2, clanB.getName());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("Error setting active war", e);
        }
    }

    @Override
    public void clearWar(Clan clanA, Clan clanB) {
        String key = generateWarKey(clanA, clanB);
        activeWars.remove(key);

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM active_wars WHERE clan1=? AND clan2=?")) {

            ps.setString(1, clanA.getName());
            ps.setString(2, clanB.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            logError("Error clearing war", e);
        }
    }

    private String generateWarKey(Clan a, Clan b) {
        return a.getName().compareTo(b.getName()) < 0 ?
                a.getName() + ":" + b.getName() :
                b.getName() + ":" + a.getName();
    }

    private void logError(String message, SQLException e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }

    @Override
    public void saveClans() {
        clans.values().forEach(this::saveClan);
    }

    @Override
    public void saveClan(Clan clan) {
        try {
            connection.setAutoCommit(false);

            updateClanData(clan);
            updateMembers(clan);
            updateWarParticipants(clan);

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                logError("Error rolling back transaction", ex);
            }
            logError("Error saving clan", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {}
        }
    }

    private void updateClanData(Clan clan) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO clans VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
                leader=VALUES(leader),
                pvp_enabled=VALUES(pvp_enabled),
                member_limit=VALUES(member_limit),
                bank=VALUES(bank),
                rubles=VALUES(rubles),
                level=VALUES(level),
                base_world=VALUES(base_world),
                base_x=VALUES(base_x),
                base_y=VALUES(base_y),
                base_z=VALUES(base_z),
                base_yaw=VALUES(base_yaw),
                base_pitch=VALUES(base_pitch)
        """)) {

            setClanParameters(ps, clan);
            ps.executeUpdate();
        }
    }

    private void setClanParameters(PreparedStatement ps, Clan clan) throws SQLException {
        ps.setString(1, clan.getName());
        ps.setString(2, clan.getLeader());
        ps.setBoolean(3, clan.isPvpEnabled());
        ps.setInt(4, clan.getMemberLimit());
        ps.setDouble(5, clan.getBank());
        ps.setDouble(6, clan.getRubles());
        ps.setInt(7, clan.getLevel());

        Location loc = clan.getBaseLocation();
        if (loc != null && loc.getWorld() != null) {
            ps.setString(8, loc.getWorld().getName());
            ps.setDouble(9, loc.getX());
            ps.setDouble(10, loc.getY());
            ps.setDouble(11, loc.getZ());
            ps.setFloat(12, loc.getYaw());
            ps.setFloat(13, loc.getPitch());
        } else {
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.DOUBLE);
            ps.setNull(10, Types.DOUBLE);
            ps.setNull(11, Types.DOUBLE);
            ps.setNull(12, Types.FLOAT);
            ps.setNull(13, Types.FLOAT);
        }
    }

    private void updateMembers(Clan clan) throws SQLException {
        try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM clan_members WHERE clan_name=?")) {

            del.setString(1, clan.getName());
            del.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO clan_members VALUES (?,?,?)")) {

            for (String member : clan.getMembers()) {
                ps.setString(1, clan.getName());
                ps.setString(2, member);
                ps.setString(3, clan.getRank(member).name());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void updateWarParticipants(Clan clan) throws SQLException {
        try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM war_participants WHERE clan_name=?")) {

            del.setString(1, clan.getName());
            del.executeUpdate();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO war_participants VALUES (?,?)")) {

            for (String participant : clan.getWarParticipants()) {
                ps.setString(1, clan.getName());
                ps.setString(2, participant);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public void removeClan(Clan clan) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM clans WHERE name=?")) {

            ps.setString(1, clan.getName());
            ps.executeUpdate();
            clans.remove(clan.getName());
            clan.getMembers().forEach(playerToClan::remove);
        } catch (SQLException e) {
            logError("Error removing clan", e);
        }
    }

    @Override
    public Clan getClan(String name) {
        return clans.get(name);
    }

    @Override
    public Clan getOwnersClan(String leaderName) {
        return clans.values().stream()
                .filter(c -> c.getLeader().equals(leaderName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Clan getMembersClan(String memberUUID) {
        return clans.get(playerToClan.get(memberUUID));
    }

    @Override
    public Map<String, Clan> getClans() {
        return new HashMap<>(clans);
    }
}