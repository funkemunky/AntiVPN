package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.bukkit.util.ConfigDefault;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BukkitConfig implements VPNConfig {
    private final ConfigDefault<String> licenseDefault = new ConfigDefault<>("",
            "license", BukkitPlugin.pluginInstance), kickStringDefault =
                    new ConfigDefault<>("Proxies are not allowed on our server",
                    "kickMessage", BukkitPlugin.pluginInstance),
            defaultDatabaseType = new ConfigDefault<>("H2",
                    "database.type", BukkitPlugin.pluginInstance),
            defaultDatabaseName = new ConfigDefault<>("kaurivpn",
                    "database.database", BukkitPlugin.pluginInstance),
            defaultMongoURL = new ConfigDefault<>("", "database.mongoURL", BukkitPlugin.pluginInstance),
            defaultUsername = new ConfigDefault<>("root",
                    "database.username", BukkitPlugin.pluginInstance),
            defaultPassword = new ConfigDefault<>("password",
                    "database.password", BukkitPlugin.pluginInstance),


            defaultIp = new ConfigDefault<>("localhost", "database.ip", BukkitPlugin.pluginInstance),
            defaultAlertMsg = new ConfigDefault<>("&8[&6KauriVPN&8] &e%player% &7has joined on a VPN/proxy" +
                    " &8(&f%reason%&8) &7in location &8(&f%city%&7, &f%country%&8)", "alerts.message",
                    BukkitPlugin.pluginInstance);
    private final ConfigDefault<Boolean> cacheResultsDefault = new ConfigDefault<>(true,
            "cachedResults", BukkitPlugin.pluginInstance),
            defaultUseCredentials = new ConfigDefault<>(true,
                    "database.useCredentials", BukkitPlugin.pluginInstance),
            defaultDatabaseEnabled = new ConfigDefault<>(false, "database.enabled",
                    BukkitPlugin.pluginInstance), defaultCommandsEnable = new ConfigDefault<>(false,
            "commands.enabled", BukkitPlugin.pluginInstance), defaultKickPlayers
            = new ConfigDefault<>(true, "kickPlayers", BukkitPlugin.pluginInstance),
            defaultAlertToStaff = new ConfigDefault<>(true, "alerts.enabled",
                    BukkitPlugin.pluginInstance),
            defaultMetrics = new ConfigDefault<>(true, "bstats", BukkitPlugin.pluginInstance);
    private final ConfigDefault<Integer>
            defaultPort = new ConfigDefault<>(-1, "database.port", BukkitPlugin.pluginInstance);
    private final ConfigDefault<List<String>> prefixWhitelistsDefault = new ConfigDefault<>(new ArrayList<>(),
            "prefixWhitelists", BukkitPlugin.pluginInstance), defaultCommands = new ConfigDefault<>(
            Collections.singletonList("kick %player% VPNs are not allowed on our server!"), "commands.execute",
            BukkitPlugin.pluginInstance),
            defBlockedCountries = new ConfigDefault<>(new ArrayList<>(), "blockedCountries",
                    BukkitPlugin.pluginInstance),
            defAllowedCountries = new ConfigDefault<>(new ArrayList<>(), "allowedCountries",
            BukkitPlugin.pluginInstance);

    private String license, kickMessage, databaseType, databaseName, mongoURL, username, password, ip, alertMsg;
    private List<String> prefixWhitelists, commands, allowedCountries, blockedCountries;
    private int port;
    private boolean cacheResults, databaseEnabled, useCredentials, commandsEnabled, kickPlayers, alertToStaff, metrics;

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
    public boolean useDatabaseCreds() {
        return useCredentials;
    }

    @Override
    public String mongoDatabaseURL() {
        return mongoURL;
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
    public List<String> allowedCountries() {
        return allowedCountries;
    }

    @Override
    public List<String> blockedCountries() {
        return blockedCountries;
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
        useCredentials = defaultUseCredentials.get();
        databaseType = defaultDatabaseType.get();
        databaseName = defaultDatabaseName.get();
        mongoURL = defaultMongoURL.get();
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
        blockedCountries = defBlockedCountries.get();
        allowedCountries = defAllowedCountries.get();
    }
}
