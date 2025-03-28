package net.sxclans.bukkit.command;

import net.lielibrary.bukkit.command.BaseCommand;

import net.sxclans.bukkit.Depend;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.menus.SuggestionsMenu;
import net.sxclans.common.clan.menus.FactorMenu;
import net.sxclans.common.clan.models.ClanInviteManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Stream;


public class UserCommands extends BaseCommand {

    public UserCommands(String name, String... aliases) {
        super(name, aliases);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            Clan clan = Depend.getClanManage().getMembersClan(player.getName());
            if (clan != null) {
                new FactorMenu(player, Depend.getInstance().getFilesManager()).open();
            } else {
                new SuggestionsMenu(Depend.getInstance().getFilesManager()).openAnvilGUI(player);
            }
            return;
        }

        final Runnable action = getRunnable(args, player);
        if (action != null) {
            action.run();
        } else {
            player.sendMessage(ChatColor.RED + "Неизвестная команда.");
        }
    }

    private static Runnable getRunnable(String[] args, Player player) {
        Map<String, Runnable> actions = Map.of(
                "accept", () -> ClanInviteManager.acceptInvite(player),
                "deny", () -> ClanInviteManager.denyInvite(player),
                "base", () -> {
                    Clan clan = Depend.getClanManage().getMembersClan(player.getName());
                    if (clan != null && clan.getBaseLocation() != null) {
                        player.teleport(clan.getBaseLocation());
                        player.sendMessage(ChatColor.GREEN + "Вы телепортированы на базу клана!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Ваш клан не имеет установленной точки базы!");
                    }
                }
        );

        String command = args[0].toLowerCase();
        Runnable action = actions.get(command);
        return action;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        Player player = (Player) sender;
        if (args.length != 1) return Collections.emptyList();

        return Stream.concat(
                        Stream.of("accept", "deny"),
                        Optional.ofNullable(Depend.getClanManage().getMembersClan(player.getName())).stream().map(clan -> "base")
                )
                .filter(completion -> completion.startsWith(args[0].toLowerCase()))
                .sorted()
                .toList();
    }
}
