package net.sxclans.common.clan.war.manager;

import net.sxclans.bukkit.Depend;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class WarListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        WarManager warManager = Depend.getClanManage().getActiveWarManager(player);

        if (warManager != null) {

            warManager.onPlayerDeath(player);
        }
    }
}