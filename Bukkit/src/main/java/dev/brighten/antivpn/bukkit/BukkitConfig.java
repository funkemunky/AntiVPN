package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.bukkit.util.ConfigDefault;

import java.util.ArrayList;
import java.util.List;

public class BukkitConfig implements VPNConfig {
    private final ConfigDefault<String> licenseDefault = new ConfigDefault<>("",
            "license", BukkitPlugin.pluginInstance), kickStringDefault =
            new ConfigDefault<>("Proxies are not allowed on our server",
                    "kickMessage", BukkitPlugin.pluginInstance);
    private final ConfigDefault<Boolean> cacheResultsDefault = new ConfigDefault<>(true,
            "cachedResults", BukkitPlugin.pluginInstance);
    private final ConfigDefault<List<String>> prefixWhitelistsDefault = new ConfigDefault<>(new ArrayList<>(),
            "prefixWhitelists", BukkitPlugin.pluginInstance);

    private String license, kickMessage;
    private List<String> prefixWhitelists;
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

    @Override
    public List<String> getPrefixWhitelists() {
        return prefixWhitelists;
    }

    public void update() {
        license = licenseDefault.get();
        kickMessage = kickStringDefault.get();
        cacheResults = cacheResultsDefault.get();
        prefixWhitelists = prefixWhitelistsDefault.get();
    }
}
