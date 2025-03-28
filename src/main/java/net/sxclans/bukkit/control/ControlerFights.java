package net.sxclans.bukkit.control;

import net.lielibrary.bukkit.Plugin;
import net.sxclans.bukkit.Depend;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

public class ControlerFights implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target) || !(event.getDamager() instanceof Player damager)) {
            return;
        }

        Optional.ofNullable(Depend.getClanManage().getMembersClan(target.getName()))
                .filter(clan -> clan.equals(Depend.getClanManage().getMembersClan(damager.getName())))
                .filter(clan -> !clan.isPvpEnabled())
                .ifPresent(clan -> {
                    event.setCancelled(true);
                    damager.sendMessage(Plugin.getWithColor().hexToMinecraftColor("&cPvP между соклановцами выключено!"));
                });
    }
}
