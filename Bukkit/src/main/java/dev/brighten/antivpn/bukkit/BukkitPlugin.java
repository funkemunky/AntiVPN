package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.command.Command;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitPlugin extends JavaPlugin {

    public static BukkitPlugin pluginInstance;

    public void onEnable() {
        pluginInstance = this;

        //Loading config
        saveDefaultConfig();

        AntiVPN.start(new BukkitConfig(), new BukkitListener(), new BukkitPlayerExecutor());

        for (Command command : AntiVPN.getInstance().getCommands()) {
            getCommand(command.parent() + (command.parent().length() > 0 ? " " : "") + command.name())
                    .setExecutor((sender, cmd, key, args) -> {
                        if(!sender.hasPermission("antivpn.command.*")
                                && !sender.hasPermission(command.permission())) {
                            sender.sendMessage(ChatColor.RED + "No permission.");
                            return true;
                        }

                        command.execute(new BukkitCommandExecutor(sender), args);

                        return true;
                    });
        }
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }
}
