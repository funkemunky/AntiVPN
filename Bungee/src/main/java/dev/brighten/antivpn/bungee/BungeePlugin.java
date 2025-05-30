package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.bungee.command.BungeeCommand;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sqllite.LiteDatabase;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;

import java.util.concurrent.TimeUnit;

public class BungeePlugin extends Plugin {

    public static BungeePlugin pluginInstance;

    @Override
    public void onEnable() {
        pluginInstance = this;

        //Setting up config
        getProxy().getLogger().info("Loading config...");


        //Loading plugin
        getProxy().getLogger().info("Starting AntiVPN services...");
        AntiVPN.start(new BungeeListener(), new BungeePlayerExecutor(), getDataFolder());

        if(AntiVPN.getInstance().getVpnConfig().metrics()) {
            getProxy().getLogger().info("Starting bStats metrics...");
            Metrics metrics = new Metrics(this, 12616);
            metrics.addCustomChart(new SimplePie("database_used", this::getDatabaseType));
            getProxy().getScheduler().schedule(this,
                    () -> AntiVPN.getInstance().checked = AntiVPN.getInstance().detections = 0,
                    10, 10, TimeUnit.MINUTES);
        }

        for (Command command : AntiVPN.getInstance().getCommands()) {
            getProxy().getPluginManager().registerCommand(pluginInstance, new BungeeCommand(command));
        }
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }

    private String getDatabaseType() {
        VPNDatabase database = AntiVPN.getInstance().getDatabase();

        if(database instanceof LiteDatabase) {
            return "SQLLite";
        }  return "No-Database";
    }
}
