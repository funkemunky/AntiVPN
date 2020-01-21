package dev.brighten.pl;

import cc.funkemunky.api.Atlas;
import cc.funkemunky.api.reflections.types.WrappedClass;
import cc.funkemunky.api.utils.MiscUtils;
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
        //We use reflection and check versions to add backwards compatibility for the time being.
        new WrappedClass(Atlas.class).getMethod("initializeScanner",
                atlasVersion.startsWith("1.6") ? JavaPlugin.class : Plugin.class, boolean.class, boolean.class)
                .invoke(atlasInstance, this, true, true);

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

        print("Save", "database");
        AntiVPN.INSTANCE.vpnAPI.database.saveDatabase();

        print(false, "threads");
        AntiVPN.INSTANCE.vpnHandler.checking.set(false);

        print(false, "handlers");
        AntiVPN.INSTANCE.vpnAPI = null;
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
