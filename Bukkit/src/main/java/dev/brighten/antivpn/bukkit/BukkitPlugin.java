package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitPlugin extends JavaPlugin {

    public static BukkitPlugin pluginInstance;

    public void onEnable() {
        pluginInstance = this;

        //Loading config
        saveDefaultConfig();

        AntiVPN.start(new BukkitConfig(), new BukkitListener());
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }
}
