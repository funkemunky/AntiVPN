package dev.brighten.antivpn.bukkit.command;

import dev.brighten.antivpn.bukkit.BukkitCommandExecutor;
import dev.brighten.antivpn.command.Command;
import lombok.val;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class BukkitCommand extends org.bukkit.command.Command {

    private final Command command;
    public BukkitCommand(Command command) {
        super(command.name(), command.description(), command.usage(), Arrays.asList(command.aliases()));

        this.command = command;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
            throws IllegalArgumentException {
        val children = command.children();

        if(children.length > 0 && args.length > 0) {
            for (Command child : children) {
                if(child.name().equalsIgnoreCase(args[0])  || Arrays.stream(child.aliases())
                        .anyMatch(alias2 -> alias2.equalsIgnoreCase(args[0]))) {
                    return child.tabComplete(new BukkitCommandExecutor(sender), alias, IntStream
                            .range(0, args.length - 1)
                            .mapToObj(i -> args[i + 1]).toArray(String[]::new));
                }
            }
        }
        return command.tabComplete(new BukkitCommandExecutor(sender), alias, args);
    }

    @Override
    public boolean execute(CommandSender sender, String s, String[] args) {
        if(!sender.hasPermission("antivpn.command.*")
                && !sender.hasPermission(command.permission())) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        val children = command.children();

        if(children.length > 0 && args.length > 0) {
            for (Command child : children) {
                if(child.name().equalsIgnoreCase(args[0])  || Arrays.stream(child.aliases())
                        .anyMatch(alias -> alias.equalsIgnoreCase(args[0]))) {
                    if(!sender.hasPermission("antivpn.command.*")
                            && !sender.hasPermission(child.permission())) {
                        sender.sendMessage(ChatColor.RED + "No permission.");
                        return true;
                    }

                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            child.execute(new BukkitCommandExecutor(sender), IntStream
                                    .range(0, args.length - 1)
                                    .mapToObj(i -> args[i + 1]).toArray(String[]::new))));
                    return true;
                }
            }
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                command.execute(new BukkitCommandExecutor(sender), args)));

        return true;
    }
}
