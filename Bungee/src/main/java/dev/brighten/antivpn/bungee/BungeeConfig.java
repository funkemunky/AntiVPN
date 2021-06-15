package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.bungee.util.ConfigDefault;

public class BungeeConfig implements VPNConfig { ;
    private final ConfigDefault<String> licenseDefault = new ConfigDefault<>("",
            "license", BungeePlugin.pluginInstance), kickStringDefault =
            new ConfigDefault<>("Proxies are not allowed on our server",
                    "kickMessage", BungeePlugin.pluginInstance);
    private final ConfigDefault<Boolean> cacheResultsDefault = new ConfigDefault<>(true,
            "cachedResults", BungeePlugin.pluginInstance);

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
