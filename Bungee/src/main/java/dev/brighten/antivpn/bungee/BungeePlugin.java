package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.bungee.util.Config;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeePlugin extends Plugin {

    public static BungeePlugin pluginInstance;

    @Getter
    private Config config;

    @Override
    public void onEnable() {
        pluginInstance = this;

        //Setting up config
        config = new Config();

        //Loading plugin
        AntiVPN.start(new BungeeConfig(), new BungeeListener());
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }
}
