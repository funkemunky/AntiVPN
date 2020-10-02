package dev.brighten.pl;

import cc.funkemunky.api.Atlas;
import cc.funkemunky.api.utils.MiscUtils;
import dev.brighten.pl.handlers.AlertsHandler;
import dev.brighten.pl.handlers.VPNHandler;
import dev.brighten.pl.vpn.VPNAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiVPN extends JavaPlugin {

    public static AntiVPN INSTANCE;

    public VPNAPI vpnAPI;

    public VPNHandler vpnHandler;
    public AlertsHandler alertsHandler;

    public void onEnable() {
        INSTANCE = this;
        enable();
    }

    public void onDisable() {
        disable();
    }

    public void enable() {
        System.out.println("Enabling Atlas hook...");
        if(Bukkit.getPluginManager().getPlugin("Atlas") == null) {
            System.out.println("Atlas not found! Disabling...");
            this.disable();
            return;
        }
        saveDefaultConfig();

        print(true, "scanner");
        Atlas.getInstance().initializeScanner(this, true, true);

        print(true, "vpn api and handlers");
        vpnAPI = new VPNAPI();
        vpnHandler = new VPNHandler();
        vpnHandler.run();

        print(true, "alerts handler");
        alertsHandler = new AlertsHandler();

        MiscUtils.printToConsole("&aCompleted startup.");
    }

    public void disable() {
        print(false, "listeners");
        HandlerList.unregisterAll(this);

        print(false, "tasks");
        Bukkit.getScheduler().cancelTasks(this);

        print(false, "threads");
        AntiVPN.INSTANCE.vpnHandler.shutdown();

        print(false, "removing commands");
        Atlas.getInstance().getCommandManager(this).unregisterCommands();

        print(false, "handlers");

        MiscUtils.printToConsole("&aCompleted shutdown.");
    }

    private void print(boolean enable, String task) {
        MiscUtils.printToConsole((enable ? "&7Enabling " : "&7Disabling ") + task + "...");
    }
}
