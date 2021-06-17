package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.bungee.util.Config;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeePlugin extends Plugin {

    public static BungeePlugin pluginInstance;

    @Getter
    private Config config;

    @Override
    public void onEnable() {
        pluginInstance = this;

        //Setting up config
        config = new Config();

        //Loading plugin
        AntiVPN.start(new BungeeConfig(), new BungeeListener(), new BungeePlayerExecutor());

        //TODO Add command functionality for BungeeCord
        /*for (Command command : AntiVPN.getInstance().getCommands()) {
            BungeeCord.getInstance().getPluginManager().registerCommand(pluginInstance, new net.md_5.bungee.api.plugin
                    .Command(command.parent() + " " + command.name(), command.permission(), command.aliases()) {
                @Override
                public void execute(CommandSender commandSender, String[] strings) {
                    if(!commandSender.hasPermission("antivpn.command.*")
                            && !commandSender.hasPermission(command.permission())) {
                        commandSender.sendMessage(new ComponentBuilder("No permission").color(ChatColor.RED)
                                .create());
                        return;
                    }

                    command.execute(new BungeeCommandExecutor(commandSender), strings);
                }
            });
        }*/
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }
}
