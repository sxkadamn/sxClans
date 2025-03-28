package net.sxclans.bukkit;

import net.lielibrary.bukkit.command.BaseCommand;
import net.sxclans.bukkit.command.UserCommands;
import net.sxclans.bukkit.control.ControlerFights;
import net.sxclans.bukkit.files.FilesManager;
import net.sxclans.common.Utility;
import net.sxclans.common.clan.base.ClanBaseManager;
import net.sxclans.common.clan.base.FileBasedClanManager;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Depend extends JavaPlugin {

    private static Depend instance;


    private static ClanBaseManager clanManage;


    private FilesManager filesManager;



    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ClanRank.loadConfig(getConfig());

        File file = new File(this.getDataFolder(), "clans");
        if (!file.exists()) file.mkdirs();

        clanManage = new FileBasedClanManager(file);
        clanManage.loadClans();

        filesManager = new FilesManager(this);

        getServer().getPluginManager().registerEvents(new ControlerFights(), this);
        BaseCommand.register(this, new UserCommands("clan"));

        Utility.checkPlugin("Vault");
        Utility.checkPlugin("lieLibrary");
    }


    @Override
    public void onDisable() {
        filesManager.saveMenuConfigs();
        clanManage.saveClans();
    }

    public FilesManager getFilesManager() {
        return filesManager;
    }

    public static ClanBaseManager getClanManage() {
        return clanManage;
    }

    public static Depend getInstance() {
        return instance;
    }
}