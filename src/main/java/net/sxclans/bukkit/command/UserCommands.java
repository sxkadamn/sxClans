package net.sxclans.bukkit.command;

import net.lielibrary.bukkit.Plugin;
import net.lielibrary.bukkit.command.BaseCommand;
import net.sxclans.bukkit.Depend;
import net.sxclans.common.clan.Clan;
import net.sxclans.common.clan.menus.SuggestionsMenu;
import net.sxclans.common.clan.menus.FactorMenu;
import net.sxclans.common.clan.models.ClanInviteManager;
import net.sxclans.common.clan.war.arena.Arena;
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
         Map<String, Runnable> actions = new HashMap<>();
        
        actions.put("accept", () -> ClanInviteManager.acceptInvite(player));
        actions.put("deny", () -> ClanInviteManager.denyInvite(player));
        actions.put("base", () -> {
            Clan clan = Depend.getClanManage().getMembersClan(player.getName());
            if (clan != null && clan.getBaseLocation() != null) {
                player.teleport(clan.getBaseLocation());
                player.sendMessage(ChatColor.GREEN + "Вы телепортированы на базу клана!");
            } else {
                player.sendMessage(ChatColor.RED + "Ваш клан не имеет установленной точки базы!");
            }
        });
        
        if (args.length >= 1) {
            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "create":
                    return () -> {
                        if (args.length == 1) {
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Specify the arena name"));
                            return;
                        }
                        Arena arena = Arena.get(args[1]);
                        if (arena != null) {
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Arena does not exist!"));
                            return;
                        }
                        Arena.add(args[1]);
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Arena created!"));
                    };
                case "setpos1":
                    return () -> {
                        if (args.length == 1) {
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Specify the arena name"));
                            return;
                        }
                        Arena arena = Arena.get(args[1]);
                        if (arena == null) {
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Arena does not exist!"));
                            return;
                        }
                        arena.setClan1Spawns(player.getLocation());
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("First position set!"));
                    };

                case "setpos2":
                    return () -> {
                        if (args.length == 1) {

                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Specify the arena name"));
                            return;
                        }
                        Arena arena = Arena.get(args[1]);
                        if (arena == null) {
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Arena does not exist!"));
                            return;
                        }
                        arena.setClan2Spawns(player.getLocation());
                        player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Second position set!"));
                    };

                case "launch":
                    return () -> {
                        if (args.length == 1) {
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Specify the arena name"));
                            return;
                        }
                        Arena arena = Arena.get(args[1]);
                        if (arena == null) {
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Arena does not exist!"));
                            return;
                        }
                        if (arena.launch()) {
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Arena launched!"));
                        } else {
                            player.sendMessage(Plugin.getWithColor().hexToMinecraftColor("Cannot launch arena. Ensure you have set the lobby, attacker spawn point, and that the map size is adequate."));
                        }
                    };
            }
        }

        return actions.getOrDefault(args[0].toLowerCase(), null);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        Player player = (Player) sender;
        if (args.length != 1) return Collections.emptyList();

        return Stream.concat(
                        Stream.of("accept", "deny", "base", "setpos1", "setpos2", "launch", "create"),
                        Optional.ofNullable(Depend.getClanManage().getMembersClan(player.getName())).stream().map(clan -> "base")
                )
                .filter(completion -> completion.startsWith(args[0].toLowerCase()))
                .sorted()
                .toList();
    }
}
