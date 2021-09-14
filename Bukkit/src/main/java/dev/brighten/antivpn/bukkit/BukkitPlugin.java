package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.bukkit.util.ConfigDefault;
import dev.brighten.antivpn.command.Command;
import lombok.val;
import net.md_5.bungee.api.ChatColor;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.MultiLineChart;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class BukkitPlugin extends JavaPlugin {

    public static BukkitPlugin pluginInstance;
    private SimpleCommandMap commandMap;
    private List<org.bukkit.command.Command> registeredCommands = new ArrayList<>();
    private SingleLineChart vpnDetections, ipsChecked;

    public void onEnable() {
        pluginInstance = this;

        //Loading config
        Bukkit.getLogger().info("Loading config...");
        saveDefaultConfig();

        Bukkit.getLogger().info("Starting AntiVPN services...");
        AntiVPN.start(new BukkitConfig(), new BukkitListener(), new BukkitPlayerExecutor());

        if(AntiVPN.getInstance().getConfig().metrics()) {
            Bukkit.getLogger().info("Starting bStats metrics...");
            Metrics metrics = new Metrics(this, 12615);
            metrics.addCustomChart(vpnDetections = new SingleLineChart("vpn_detections",
                    () -> AntiVPN.getInstance().detections));
            metrics.addCustomChart(ipsChecked = new SingleLineChart("ips_checked",
                    () -> AntiVPN.getInstance().checked));
            new BukkitRunnable() {
                public void run() {
                    AntiVPN.getInstance().checked = AntiVPN.getInstance().detections = 0;
                }
            }.runTaskTimerAsynchronously(this, 12000, 12000);
        }

        Bukkit.getLogger().info("Setting up and registering commands...");
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
            val newCommand = new org.bukkit.command.Command(command.name(), command.description(), command.usage(),
                    Arrays.asList(command.aliases())) {
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
            };

            registeredCommands.add(newCommand);
            commandMap.register(pluginInstance.getName(), newCommand);
        }

        AntiVPN.getInstance().getMessageHandler().initStrings(vpnString -> new ConfigDefault<>
                (vpnString.getDefaultMessage(), "messages." + vpnString.getKey(), BukkitPlugin.pluginInstance)
                .get());
        //TODO Finish system before implementing on startup
        /*Bukkit.getLogger().info("Getting strings...");
        AntiVPN.getInstance().getMessageHandler().initStrings(vpnString -> new ConfigDefault<>
                (vpnString.getDefaultMessage(), "messages." + vpnString.getKey(), BukkitPlugin.pluginInstance)
                .get());
        AntiVPN.getInstance().getMessageHandler().reloadStrings();*/
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().info("Stopping plugin services...");
        AntiVPN.getInstance().stop();

        Bukkit.getLogger().info("Unregistering commands...");
        try {
            Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);
            Map<String, org.bukkit.command.Command> knownCommands =
                    (Map<String, org.bukkit.command.Command>) field.get(commandMap);

            knownCommands.values().removeAll(registeredCommands);
            registeredCommands.clear();
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        Bukkit.getLogger().info("Unregistering listeners...");
        HandlerList.unregisterAll(this);

        Bukkit.getLogger().info("Cancelling any running tasks...");
        Bukkit.getScheduler().cancelTasks(this);
    }
}
