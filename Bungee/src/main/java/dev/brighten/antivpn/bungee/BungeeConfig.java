package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.bungee.util.ConfigDefault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BungeeConfig implements VPNConfig {
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
            defaultIp = new ConfigDefault<>("localhost", "database.ip", BungeePlugin.pluginInstance),
            defaultAlertMsg = new ConfigDefault<>("&8[&6KauriVPN&8] &e%player% &7has joined on a VPN/proxy" +
                    " &8(&f%reason%&8) &7in location &8(&f%city%&7, &f%country%&8)", "alerts.message",
                    BungeePlugin.pluginInstance);
    private final ConfigDefault<Boolean> cacheResultsDefault = new ConfigDefault<>(true,
            "cachedResults", BungeePlugin.pluginInstance),
            defaultDatabaseEnabled = new ConfigDefault<>(false, "database.enabled",
                    BungeePlugin.pluginInstance), defaultCommandsEnable = new ConfigDefault<>(false,
            "commands.enabled", BungeePlugin.pluginInstance), defaultKickPlayers
            = new ConfigDefault<>(true, "kickPlayers", BungeePlugin.pluginInstance),
            defaultAlertToStaff = new ConfigDefault<>(true, "alerts.enabled",
                    BungeePlugin.pluginInstance),
            defaultMetrics = new ConfigDefault<>(true, "bstats", BungeePlugin.pluginInstance);
    private final ConfigDefault<Integer>
            defaultPort = new ConfigDefault<>(-1, "database.port", BungeePlugin.pluginInstance);
    private final ConfigDefault<List<String>> prefixWhitelistsDefault = new ConfigDefault<>(new ArrayList<>(),
            "prefixWhitelists", BungeePlugin.pluginInstance), defaultCommands = new ConfigDefault<>(
            Collections.singletonList("kick %player% VPNs are not allowed on our server!"), "commands.execute",
            BungeePlugin.pluginInstance);

    private String license, kickMessage, databaseType, databaseName, username, password, ip, alertMsg;
    private List<String> prefixWhitelists, commands;
    private int port;
    private boolean cacheResults, databaseEnabled, commandsEnabled, kickPlayers, alertToStaff, metrics;

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
    
    public boolean isBukkit() {
        return false;
    }

    @Override
    public String alertMessage() {
        return alertMsg;
    }

    @Override
    public boolean alertToStaff() {
        return alertToStaff;
    }

    @Override
    public boolean runCommands() {
        return commandsEnabled;
    }

    @Override
    public List<String> commands() {
        return commands;
    }

    @Override
    public boolean kickPlayersOnDetect() {
        return kickPlayers;
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

    @Override
    public boolean metrics() {
        return metrics;
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
        commandsEnabled = defaultCommandsEnable.get();
        commands = defaultCommands.get();
        kickPlayers = defaultKickPlayers.get();
        alertToStaff = defaultAlertToStaff.get();
        alertMsg = defaultAlertMsg.get();
        metrics = defaultMetrics.get();
    }
}
