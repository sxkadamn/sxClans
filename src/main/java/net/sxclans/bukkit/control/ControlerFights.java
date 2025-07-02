package net.sxclans.bukkit.control;

import net.lielibrary.bukkit.Plugin;
import net.sxclans.bukkit.Depend;
import net.sxclans.common.clan.Clan;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Optional;

public class ControlerFights implements Listener {


    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)
                || !(event.getDamager() instanceof Player damager)) {
            return;
        }

        Optional.ofNullable(Depend.getClanManage().getMembersClan(target.getName()))
                .filter(clan -> clan.equals(Depend.getClanManage().getMembersClan(damager.getName())))
                .filter(clan -> !clan.isPvpEnabled())
                .ifPresent(clan -> {
                    event.setCancelled(true);
                    damager.sendMessage(Plugin.getWithColor().hexToMinecraftColor(
                            Depend.getInstance().getConfig().getString("messages.pvp_disabled")
                    ));
                });
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;

        Player killer = event.getEntity().getKiller();
        Clan clan = Depend.getClanManage().getMembersClan(killer.getName());
        if (clan != null) {
            clan.addLevel(0.25);
        }
    }
}
