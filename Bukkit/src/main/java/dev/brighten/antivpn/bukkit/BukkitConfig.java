package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.bukkit.util.ConfigDefault;

public class BukkitConfig implements VPNConfig {
    private final ConfigDefault<String> licenseDefault = new ConfigDefault<>("",
            "license", BukkitPlugin.pluginInstance), kickStringDefault =
            new ConfigDefault<>("Proxies are not allowed on our server",
                    "kickMessage", BukkitPlugin.pluginInstance);
    private final ConfigDefault<Boolean> cacheResultsDefault = new ConfigDefault<>(true,
            "cachedResults", BukkitPlugin.pluginInstance);

    private String license, kickMessage;
    private boolean cacheResults;

    @Override
    public String getLicense() {
        return license;
    }

    @Override
    public boolean cachedResults() {
        return cacheResults;
    }

    @Override
    public String getKickString() {
        return kickMessage;
    }

    public void update() {
        license = licenseDefault.get();
        kickMessage = kickStringDefault.get();
        cacheResults = cacheResultsDefault.get();
    }
}
