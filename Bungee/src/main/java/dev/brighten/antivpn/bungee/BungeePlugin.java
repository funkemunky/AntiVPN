package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.bungee.command.BungeeCommand;
import dev.brighten.antivpn.command.Command;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;

import java.util.concurrent.TimeUnit;

public class BungeePlugin extends Plugin {

    public static BungeePlugin pluginInstance;

    private SingleLineChart vpnDetections, ipsChecked;

    @Override
    public void onEnable() {
        pluginInstance = this;

        //Setting up config
        BungeeCord.getInstance().getLogger().info("Loading config...");


        //Loading plugin
        BungeeCord.getInstance().getLogger().info("Starting AntiVPN services...");
        AntiVPN.start(new BungeeListener(), new BungeePlayerExecutor(), getDataFolder());

        if(AntiVPN.getInstance().getVpnConfig().metrics()) {
            BungeeCord.getInstance().getLogger().info("Starting bStats metrics...");
            Metrics metrics = new Metrics(this, 12616);
            metrics.addCustomChart(vpnDetections = new SingleLineChart("vpn_detections",
                    () -> AntiVPN.getInstance().detections));
            metrics.addCustomChart(ipsChecked = new SingleLineChart("ips_checked",
                    () -> AntiVPN.getInstance().checked));
            BungeeCord.getInstance().getScheduler().schedule(this,
                    () -> AntiVPN.getInstance().checked = AntiVPN.getInstance().detections = 0,
                    10, 10, TimeUnit.MINUTES);
        }

        for (Command command : AntiVPN.getInstance().getCommands()) {
            BungeeCord.getInstance().getPluginManager().registerCommand(pluginInstance, new BungeeCommand(command));
        }
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }
}
