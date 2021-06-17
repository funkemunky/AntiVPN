package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.command.Command;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.Arrays;

public class BukkitPlugin extends JavaPlugin {

    public static BukkitPlugin pluginInstance;
    private SimpleCommandMap commandMap;

    public void onEnable() {
        pluginInstance = this;

        //Loading config
        saveDefaultConfig();

        AntiVPN.start(new BukkitConfig(), new BukkitListener(), new BukkitPlayerExecutor());

        if (pluginInstance.getServer().getPluginManager() instanceof SimplePluginManager) {
            SimplePluginManager manager = (SimplePluginManager) pluginInstance.getServer().getPluginManager();
            try {
                Field field = SimplePluginManager.class.getDeclaredField("commandMap");
                field.setAccessible(true);
                commandMap = (SimpleCommandMap) field.get(manager);
            } catch (IllegalArgumentException | SecurityException | NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        for (Command command : AntiVPN.getInstance().getCommands()) {
            commandMap.register("antivpn", new org.bukkit.command.Command(command.name(),
                    command.description(), command.usage(), Arrays.asList(command.aliases())) {
                @Override
                public boolean execute(CommandSender sender, String s, String[] args) {
                        if(!sender.hasPermission("antivpn.command.*")
                                && !sender.hasPermission(command.permission())) {
                            sender.sendMessage(ChatColor.RED + "No permission.");
                            return true;
                        }

                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                command.execute(new BukkitCommandExecutor(sender), args)));

                    return true;
                }
            });
        }
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }
}
