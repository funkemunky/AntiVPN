package dev.brighten.antivpn.bungee.command;

import dev.brighten.antivpn.AntiVPN;
import lombok.val;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.stream.IntStream;

public class BungeeCommand extends Command implements TabExecutor {

    private final dev.brighten.antivpn.command.Command command;
    public BungeeCommand(dev.brighten.antivpn.command.Command command) {
        super(command.name(), command.permission(), command.aliases());

        this.command = command;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(!sender.hasPermission("antivpn.command.*")
                && !sender.hasPermission(command.permission())) {
            sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                    AntiVPN.getInstance().getMessageHandler().getString("no-permission").getMessage())));
            return;
        }

        val children = command.children();

        if(children.length > 0 && args.length > 0) {
            for (dev.brighten.antivpn.command.Command child : children) {
                if(child.name().equalsIgnoreCase(args[0]) || Arrays.stream(child.aliases())
                        .anyMatch(alias -> alias.equalsIgnoreCase(args[0]))) {
                    if(!sender.hasPermission("antivpn.command.*")
                            && !sender.hasPermission(child.permission())) {
                        sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                                AntiVPN.getInstance().getMessageHandler().getString("no-permission").getMessage())));
                        return;
                    }

                    sender.sendMessage(TextComponent
                            .fromLegacyText(ChatColor
                                    .translateAlternateColorCodes('&',
                                            child.execute(new BungeeCommandExecutor(sender),  IntStream
                                                    .range(0, args.length - 1)
                                                    .mapToObj(i -> args[i + 1]).toArray(String[]::new)))));
                    return;
                }
            }
        }


        sender.sendMessage(TextComponent
                .fromLegacyText(ChatColor
                        .translateAlternateColorCodes('&',
                                command.execute(new BungeeCommandExecutor(sender), args))));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        val children = command.children();

        if(children.length > 0 && args.length > 0) {
            for (dev.brighten.antivpn.command.Command child : children) {
                if(child.name().equalsIgnoreCase(args[0])  || Arrays.stream(child.aliases())
                        .anyMatch(alias2 -> alias2.equalsIgnoreCase(args[0]))) {
                    return child.tabComplete(new BungeeCommandExecutor(sender), "alias", IntStream
                            .range(0, args.length - 1)
                            .mapToObj(i -> args[i + 1]).toArray(String[]::new));
                }
            }
        }
        return command.tabComplete(new BungeeCommandExecutor(sender), "alias", args);
    }
}
