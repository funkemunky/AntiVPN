package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.bungee.util.Config;
import dev.brighten.antivpn.command.Command;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.stream.IntStream;

public class BungeePlugin extends Plugin {

    public static BungeePlugin pluginInstance;

    @Getter
    private Config config;

    private static BaseComponent[] noPermission = new ComponentBuilder("No permission").color(ChatColor.RED)
            .create();

    @Override
    public void onEnable() {
        pluginInstance = this;

        //Setting up config
        config = new Config();

        //Loading plugin
        AntiVPN.start(new BungeeConfig(), new BungeeListener(), new BungeePlayerExecutor());

        //TODO Add command functionality for BungeeCord
        for (Command command : AntiVPN.getInstance().getCommands()) {
            BungeeCord.getInstance().getPluginManager().registerCommand(pluginInstance, new net.md_5.bungee.api.plugin
                    .Command(command.name(), command.permission(), command.aliases()) {

                @Override
                public void execute(CommandSender sender, String[] args) {
                    if(!sender.hasPermission("antivpn.command.*")
                            && !sender.hasPermission(command.permission())) {
                        sender.sendMessage(noPermission);
                        return;
                    }

                    val children = command.children();

                    if(children.length > 0 && args.length > 0) {
                        for (Command child : children) {
                            if(child.name().equalsIgnoreCase(args[0])) {
                                if(!sender.hasPermission("antivpn.command.*")
                                        && !sender.hasPermission(child.permission())) {
                                    sender.sendMessage(noPermission);
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
            });
        }
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }
}
