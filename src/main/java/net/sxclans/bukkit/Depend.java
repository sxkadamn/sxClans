package net.sxclans.bukkit;

import net.lielibrary.bukkit.command.BaseCommand;
import net.lielibrary.gui.impl.MenuListener;
import net.sxclans.bukkit.command.UserCommands;
import net.sxclans.bukkit.control.ControlerFights;
import net.sxclans.bukkit.files.FilesManager;
import net.sxclans.bukkit.holograms.HologramManager;
import net.sxclans.common.Utility;
import net.sxclans.common.clan.base.ClanBaseManager;
import net.sxclans.common.clan.base.FileBasedClanManager;
import net.sxclans.common.clan.base.MySQLClanManager;
import net.sxclans.common.clan.base.PostgreSQLClanManager;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Depend extends JavaPlugin {

    private static Depend instance;


    private static ClanBaseManager clanManage;


    private FilesManager filesManager;

    private HologramManager hologramManager;



    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        Utility.checkPlugin("lieLibrary");
        Utility.checkPlugin("DecentHolograms");
        Utility.checkPlugin("Vault");

        ClanRank.loadConfig(getConfig());


        String type = getConfig().getString("storage.type").toLowerCase();
        switch (type) {
            case "mysql" -> {
                String host = getConfig().getString("mysql.host");
                int port = getConfig().getInt("mysql.port");
                String database = getConfig().getString("mysql.database");
                String user = getConfig().getString("mysql.user");
                String password = getConfig().getString("mysql.password");

                Connection conn;
                try {
                    conn = DriverManager.getConnection(
                            "jdbc:mysql://" + host + ":" + port + "/" + database,
                            user, password);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                clanManage = new MySQLClanManager(conn);
            }
            case "postgresql", "postgres" -> {
                String host = getConfig().getString("postgres.host");
                int port = getConfig().getInt("postgres.port");
                String database = getConfig().getString("postgres.database");
                String user = getConfig().getString("postgres.user");
                String password = getConfig().getString("postgres.password");

                Connection conn;
                try {
                    conn = DriverManager.getConnection(
                            "jdbc:postgresql://" + host + ":" + port + "/" + database,
                            user, password
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                clanManage = new PostgreSQLClanManager(conn);
            }
            default -> {
                File file = new File(this.getDataFolder(), "clans");
                if (!file.exists()) file.mkdirs();

                clanManage = new FileBasedClanManager(file);
            }
        }

        clanManage.loadClans();
        filesManager = new FilesManager(this);

        hologramManager = new HologramManager(this);
        hologramManager.init();

        getServer().getPluginManager().registerEvents(new ControlerFights(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        BaseCommand.register(this, new UserCommands("clan"));
    }



    @Override
    public void onDisable() {
        filesManager.saveMenuConfigs();
        clanManage.saveClans();
        hologramManager.disable();
    }


    public static Depend getInstance() {
        return instance;
    }


    public static ClanBaseManager getClanManage() {
        return clanManage;
    }


    public FilesManager getFilesManager() {
        return filesManager;
    }

    public void setFilesManager(FilesManager filesManager) {
        this.filesManager = filesManager;
    }

}