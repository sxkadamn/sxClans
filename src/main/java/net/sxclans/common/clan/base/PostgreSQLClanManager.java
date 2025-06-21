package net.sxclans.common.clan.base;

import net.sxclans.bukkit.Depend;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.models.ClanRank;
import net.sxclans.common.clan.war.manager.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PostgreSQLClanManager implements ClanBaseManager {
    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private final Map<String, String> playerToClan = new ConcurrentHashMap<>();
    private final Map<String, String> warRequests = new ConcurrentHashMap<>();
    private final Map<String, WarManager> activeWars = new ConcurrentHashMap<>();
    private final Connection connection;

    public PostgreSQLClanManager(Connection connection) {
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
                    name TEXT PRIMARY KEY,
                    leader TEXT NOT NULL,
                    pvp_enabled BOOLEAN NOT NULL,
                    member_limit INTEGER NOT NULL,
                    bank DOUBLE PRECISION NOT NULL,
                    rubles DOUBLE PRECISION NOT NULL,
                    level INTEGER NOT NULL,
                    base_world TEXT,
                    base_x DOUBLE PRECISION,
                    base_y DOUBLE PRECISION,
                    base_z DOUBLE PRECISION,
                    base_yaw REAL,
                    base_pitch REAL
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS clan_members (
                    clan_name TEXT REFERENCES clans(name) ON DELETE CASCADE,
                    player_uuid TEXT NOT NULL,
                    rank TEXT NOT NULL,
                    PRIMARY KEY (clan_name, player_uuid)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_requests (
                    sender_clan TEXT REFERENCES clans(name) ON DELETE CASCADE,
                    receiver_clan TEXT REFERENCES clans(name) ON DELETE CASCADE,
                    PRIMARY KEY (sender_clan, receiver_clan)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS active_wars (
                    clan1 TEXT REFERENCES clans(name) ON DELETE CASCADE,
                    clan2 TEXT REFERENCES clans(name) ON DELETE CASCADE,
                    start_time BIGINT NOT NULL,
                    PRIMARY KEY (clan1, clan2)
                )
            """);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAllData() {
        loadClans();
        loadMembers();
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

                clan.setPvpEnabled(rs.getBoolean("pvp_enabled"));
                clan.setMemberLimit(rs.getInt("member_limit"));
                clan.setBank(rs.getDouble("bank"));
                clan.setRubles(rs.getDouble("rubles"));
                clan.setLevel(rs.getInt("level"));

                Location loc = parseLocation(rs);
                if (loc != null) clan.setBaseLocation(loc);

                clans.put(clan.getName(), clan);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Location parseLocation(ResultSet rs) throws SQLException {
        World world = Bukkit.getWorld(rs.getString("base_world"));
        if (world == null) return null;

        return new Location(
                world,
                rs.getDouble("base_x"),
                rs.getDouble("base_y"),
                rs.getDouble("base_z"),
                rs.getFloat("base_yaw"),
                rs.getFloat("base_pitch")
        );
    }

    private void loadMembers() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM clan_members");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Clan clan = clans.get(rs.getString("clan_name"));
                if (clan == null) continue;

                String uuid = rs.getString("player_uuid");
                clan.getMembers().add(uuid);
                clan.getMemberRanks().put(uuid,
                        ClanRank.valueOf(rs.getString("rank")));
                playerToClan.put(uuid, clan.getName());
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    private void loadActiveWars() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM active_wars");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Clan clan1 = clans.get(rs.getString("clan1"));
                Clan clan2 = clans.get(rs.getString("clan2"));
                if (clan1 == null || clan2 == null) continue;

                WarManager war = new WarManager(
                        clan1,
                        clan2
                );
                activeWars.put(clan1.getName() + ":" + clan2.getName(), war);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveClans() {
        clans.values().forEach(this::saveClan);
    }

    @Override
    public void saveClan(Clan clan) {
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO clans VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT (name) DO UPDATE SET
                    leader = EXCLUDED.leader,
                    pvp_enabled = EXCLUDED.pvp_enabled,
                    member_limit = EXCLUDED.member_limit,
                    bank = EXCLUDED.bank,
                    rubles = EXCLUDED.rubles,
                    level = EXCLUDED.level,
                    base_world = EXCLUDED.base_world,
                    base_x = EXCLUDED.base_x,
                    base_y = EXCLUDED.base_y,
                    base_z = EXCLUDED.base_z,
                    base_yaw = EXCLUDED.base_yaw,
                    base_pitch = EXCLUDED.base_pitch
            """)) {
                setClanParameters(ps, clan);
                ps.executeUpdate();
            }

            updateMembers(clan);

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
            ps.setNull(12, Types.REAL);
            ps.setNull(13, Types.REAL);
        }
    }

    private void updateMembers(Clan clan) throws SQLException {
        try (PreparedStatement del = connection.prepareStatement(
                "DELETE FROM clan_members WHERE clan_name = ?")) {

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

    @Override
    public void removeClan(Clan clan) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM clans WHERE name = ?")) {

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
                .filter(c -> c.getLeader().equals(leaderName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Clan getMembersClan(String memberName) {
        return clans.get(playerToClan.get(memberName));
    }

    @Override
    public void addWarRequest(String sender, String receiver) {
        warRequests.put(receiver, sender);

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO war_requests VALUES (?,?) ON CONFLICT DO NOTHING")) {

            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        WarManager war = new WarManager(clan1, clan2);
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
            e.printStackTrace();
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
        return activeWars.values().stream()
                .filter(war -> war.isParticipant(player))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void setActiveWarManager(Clan a, Clan b, WarManager war) {
        String key = a.getName() + ":" + b.getName();
        activeWars.put(key, war);

        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO active_wars VALUES (?,?,?)
            ON CONFLICT (clan1, clan2) DO UPDATE SET
                start_time = EXCLUDED.start_time
        """)) {

            ps.setString(1, a.getName());
            ps.setString(2, b.getName());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clearWar(Clan a, Clan b) {
        String key = a.getName() + ":" + b.getName();
        activeWars.remove(key);

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM active_wars WHERE clan1 = ? AND clan2 = ?")) {

            ps.setString(1, a.getName());
            ps.setString(2, b.getName());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}