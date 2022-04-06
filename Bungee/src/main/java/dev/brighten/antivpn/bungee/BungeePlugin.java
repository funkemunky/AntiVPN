package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.bungee.command.BungeeCommand;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.utils.ConfigDefault;
import lombok.val;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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

        BungeeCord.getInstance().getLogger().info("Getting strings...");
        AntiVPN.getInstance().getMessageHandler().initStrings(vpnString -> new ConfigDefault<>
                (vpnString.getDefaultMessage(), "messages." + vpnString.getKey(), AntiVPN.getInstance())
                .get());
        //TODO Finish system before implementing on startup
        /*BungeeCord.getInstance().getLogger().info("Getting strings...");
        AntiVPN.getInstance().getMessageHandler().initStrings(vpnString -> new ConfigDefault<>
                (vpnString.getDefaultMessage(), "messages." + vpnString.getKey(), BungeePlugin.pluginInstance)
                .get());
        AntiVPN.getInstance().getMessageHandler().reloadStrings();*/
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }
}
