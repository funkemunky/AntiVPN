package dev.brighten.pl;

import cc.funkemunky.api.Atlas;
import cc.funkemunky.api.reflections.types.WrappedClass;
import cc.funkemunky.api.utils.MiscUtils;
import cc.funkemunky.api.utils.RunUtils;
import dev.brighten.pl.data.UserData;
import dev.brighten.pl.handlers.AlertsHandler;
import dev.brighten.pl.handlers.VPNHandler;
import dev.brighten.pl.vpn.VPNAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiVPN extends JavaPlugin {

    public static AntiVPN INSTANCE;

    public VPNAPI vpnAPI;

    public VPNHandler vpnHandler;
    public AlertsHandler alertsHandler;
    public String atlasVersion;
    public Plugin atlasInstance;

    public void onEnable() {
        INSTANCE = this;
        enable();
    }

    public void onDisable() {
        disable();
    }

    public void enable() {
        System.out.println("Enabling Atlas hook...");
        if((atlasInstance = Bukkit.getPluginManager().getPlugin("Atlas")) != null) {
            atlasVersion = atlasInstance.getDescription().getVersion();
        } else {
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

        RunUtils.taskTimer(() -> {
            UserData.dataMap.values().stream()
                    .filter(user -> !user.getPlayer().hasPermission("kvpn.bypass")
                            && user.response != null && user.response.isProxy())
                    .forEach(user -> user.getPlayer().kickPlayer("not checked"));
        }, this, 20L, 40L);
    }

    public void disable() {
        print(false, "listeners");
        HandlerList.unregisterAll(this);

        print(false, "tasks");
        Bukkit.getScheduler().cancelTasks(this);

        print("Save", "database");

        print(false, "threads");
        AntiVPN.INSTANCE.vpnAPI.vpnThread.shutdownNow();

        print(false, "handlers");
        AntiVPN.INSTANCE.vpnAPI.vpnThread = null;
        AntiVPN.INSTANCE.vpnHandler = null;
        AntiVPN.INSTANCE.alertsHandler = null;

        MiscUtils.printToConsole("&aCompleted shutdown.");
    }

    private void print(boolean enable, String task) {
        MiscUtils.printToConsole((enable ? "&7Enabling " : "&7Disabling ") + task + "...");
    }

    private void print(String custom, String task) {
        MiscUtils.printToConsole("&7" + custom + "ing " + task + "...");
    }
}
