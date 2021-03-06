package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.bungee.util.ConfigDefault;

import java.util.ArrayList;
import java.util.List;

public class BungeeConfig implements VPNConfig { ;
    private final ConfigDefault<String> licenseDefault = new ConfigDefault<>("",
            "license", BungeePlugin.pluginInstance), kickStringDefault =
            new ConfigDefault<>("Proxies are not allowed on our server",
                    "kickMessage", BungeePlugin.pluginInstance),
            defaultDatabaseType = new ConfigDefault<>("MySQL",
                    "database.type", BungeePlugin.pluginInstance),
            defaultDatabaseName = new ConfigDefault<>("kaurivpn",
                    "database.database", BungeePlugin.pluginInstance),
            defaultUsername = new ConfigDefault<>("root",
                    "database.username", BungeePlugin.pluginInstance),
            defaultPassword = new ConfigDefault<>("password",
                    "database.password", BungeePlugin.pluginInstance),
            defaultAuthDatabase = new ConfigDefault<>("admin",
                    "database.auth", BungeePlugin.pluginInstance),
            defaultIp = new ConfigDefault<>("localhost", "database.ip", BungeePlugin.pluginInstance);
    private final ConfigDefault<Boolean> cacheResultsDefault = new ConfigDefault<>(true,
            "cachedResults", BungeePlugin.pluginInstance),
            defaultDatabaseEnabled = new ConfigDefault<>(false, "database.enabled",
                    BungeePlugin.pluginInstance);
    private final ConfigDefault<Integer>
            defaultPort = new ConfigDefault<>(-1, "database.port", BungeePlugin.pluginInstance);
    private final ConfigDefault<List<String>> prefixWhitelistsDefault = new ConfigDefault<>(new ArrayList<>(),
            "prefixWhitelists", BungeePlugin.pluginInstance);

    private String license, kickMessage, databaseType, databaseName, username, password, ip;
    private List<String> prefixWhitelists;
    private int port;
    private boolean cacheResults, databaseEnabled;

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

    @Override
    public boolean isDatabaseEnabled() {
        return databaseEnabled;
    }

    @Override
    public String getDatabaseType() {
        return databaseType;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public int getPort() {
        if(port == -1) {
            switch (getDatabaseType().toLowerCase()) {
                case "mongodb":
                case "mongo":
                case "mongod":
                    return 27017;
                case "sql":
                case "mysql":
                    return 3306;
            }
        }

        return port;
    }

    public void update() {
        license = licenseDefault.get();
        kickMessage = kickStringDefault.get();
        cacheResults = cacheResultsDefault.get();
        prefixWhitelists = prefixWhitelistsDefault.get();
        databaseEnabled = defaultDatabaseEnabled.get();
        databaseType = defaultDatabaseType.get();
        databaseName = defaultDatabaseName.get();
        username = defaultUsername.get();
        password = defaultPassword.get();
        ip = defaultIp.get();
        port = defaultPort.get();
    }
}
