package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.bukkit.command.BukkitCommand;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sqllite.LiteDatabase;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BukkitPlugin extends JavaPlugin {

    public static BukkitPlugin pluginInstance;
    private SimpleCommandMap commandMap;
    private final List<org.bukkit.command.Command> registeredCommands = new ArrayList<>();

    @Getter
    private PlayerCommandRunner playerCommandRunner;

    public void onEnable() {
        pluginInstance = this;

        Bukkit.getLogger().info("Starting AntiVPN services...");
        AntiVPN.start(new BukkitListener(), new BukkitPlayerExecutor(), getDataFolder());

        playerCommandRunner = new PlayerCommandRunner();
        playerCommandRunner.start();

        // Loading our bStats metrics to be pushed to https://bstats.org
        if(AntiVPN.getInstance().getVpnConfig().isMetrics()) {
            Bukkit.getLogger().info("Starting bStats metrics...");
            Metrics metrics = new Metrics(this, 12615);
            metrics.addCustomChart(new SimplePie("database_used", this::getDatabaseType));
            new BukkitRunnable() {
                public void run() {
                    AntiVPN.getInstance().checked = AntiVPN.getInstance().detections = 0;
                }
            }.runTaskTimerAsynchronously(this, 12000, 12000);
        }

        Bukkit.getLogger().info("Setting up and registering commands...");
        // We need access to the commandMap to register our commands without using the "proper" method
        if (pluginInstance.getServer().getPluginManager() instanceof SimplePluginManager manager) {
            try {
                Field field = SimplePluginManager.class.getDeclaredField("commandMap");
                field.setAccessible(true);
                commandMap = (SimpleCommandMap) field.get(manager);
            } catch (IllegalArgumentException | SecurityException | NoSuchFieldException | IllegalAccessException e) {
                AntiVPN.getInstance().getExecutor().logException(e);
            }
        }

        // Registering commands
        for (Command command : AntiVPN.getInstance().getCommands()) {
            // Wraps our general command API to Bukkit specific calls
            BukkitCommand newCommand = new BukkitCommand(command);

            // Adding to our own list for later referencing
            registeredCommands.add(newCommand);

            // This tells Bukkit to register our command for use.
            commandMap.register(pluginInstance.getName(), newCommand);
        }

        //TODO Finish system before implementing on startup
        /*Bukkit.getLogger().info("Getting strings...");
        AntiVPN.getInstance().getMessageHandler().initStrings(vpnString -> new ConfigDefault<>
                (vpnString.getDefaultMessage(), "messages." + vpnString.getKey(), BukkitPlugin.pluginInstance)
                .get());
        AntiVPN.getInstance().getMessageHandler().reloadStrings();*/

        reloadConfig();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onDisable() {
        Bukkit.getLogger().info("Stopping plugin services...");
        AntiVPN.getInstance().stop();
        playerCommandRunner.stop();

        Bukkit.getLogger().info("Unregistering commands...");
        try {
            Field field = SimpleCommandMap.class.getDeclaredField("knownCommands");
            field.setAccessible(true);

            if(field.get(commandMap) instanceof Map<?, ?> knownCommands) {
                Map<String, org.bukkit.command.Command> casted = (Map<String, org.bukkit.command.Command>) knownCommands;
                casted.values().removeAll(registeredCommands);
                registeredCommands.clear();
            }

        } catch (IllegalAccessException | NoSuchFieldException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }

        Bukkit.getLogger().info("Unregistering listeners...");
        HandlerList.unregisterAll(this);

        Bukkit.getLogger().info("Cancelling any running tasks...");
        Bukkit.getScheduler().cancelTasks(this);
    }

    private String getDatabaseType() {
        VPNDatabase database = AntiVPN.getInstance().getDatabase();

        if(database instanceof LiteDatabase) {
            return "SQLLite";
        }  return "No-Database";
    }
}
