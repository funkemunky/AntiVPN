package dev.brighten.antivpn.api;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.ConfigDefault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VPNConfig {
    private final ConfigDefault<String> licenseDefault = new ConfigDefault<>("",
            "license", AntiVPN.getInstance()), kickStringDefault =
            new ConfigDefault<>("Proxies are not allowed on our server",
                    "kickMessage", AntiVPN.getInstance()),
            defaultDatabaseType = new ConfigDefault<>("H2",
                    "database.type", AntiVPN.getInstance()),
            defaultDatabaseName = new ConfigDefault<>("kaurivpn",
                    "database.database", AntiVPN.getInstance()),
            defaultMongoURL = new ConfigDefault<>("", "database.mongoURL", AntiVPN.getInstance()),
            defaultUsername = new ConfigDefault<>("root",
                    "database.username", AntiVPN.getInstance()),
            defaultPassword = new ConfigDefault<>("password",
                    "database.password", AntiVPN.getInstance()),


    defaultIp = new ConfigDefault<>("localhost", "database.ip", AntiVPN.getInstance()),
            defaultAlertMsg = new ConfigDefault<>("&8[&6KauriVPN&8] &e%player% &7has joined on a VPN/proxy" +
                    " &8(&f%reason%&8) &7in location &8(&f%city%&7, &f%country%&8)", "alerts.message",
                    AntiVPN.getInstance());
    private final ConfigDefault<Boolean> cacheResultsDefault = new ConfigDefault<>(true,
            "cachedResults", AntiVPN.getInstance()),
            defaultUseCredentials = new ConfigDefault<>(true,
                    "database.useCredentials", AntiVPN.getInstance()),
            defaultDatabaseEnabled = new ConfigDefault<>(false, "database.enabled",
                    AntiVPN.getInstance()), defaultCommandsEnable = new ConfigDefault<>(false,
            "commands.enabled", AntiVPN.getInstance()), defaultKickPlayers
            = new ConfigDefault<>(true, "kickPlayers", AntiVPN.getInstance()),
            defaultAlertToStaff = new ConfigDefault<>(true, "alerts.enabled",
                    AntiVPN.getInstance()),
            defaultMetrics = new ConfigDefault<>(true, "bstats", AntiVPN.getInstance());
    private final ConfigDefault<Integer>
            defaultPort = new ConfigDefault<>(-1, "database.port", AntiVPN.getInstance());
    private final ConfigDefault<List<String>> prefixWhitelistsDefault = new ConfigDefault<>(new ArrayList<>(),
            "prefixWhitelists", AntiVPN.getInstance()), defaultCommands = new ConfigDefault<>(
            Collections.singletonList("kick %player% VPNs are not allowed on our server!"), "commands.execute",
            AntiVPN.getInstance()),
            defBlockedCountries = new ConfigDefault<>(new ArrayList<>(), "blockedCountries",
                    AntiVPN.getInstance()),
            defAllowedCountries = new ConfigDefault<>(new ArrayList<>(), "allowedCountries",
                    AntiVPN.getInstance());

    private String license, kickMessage, databaseType, databaseName, mongoURL, username, password, ip, alertMsg;
    private List<String> prefixWhitelists, commands, allowedCountries, blockedCountries;
    private int port;
    private boolean cacheResults, databaseEnabled, useCredentials, commandsEnabled, kickPlayers, alertToStaff, metrics;

    public String getLicense() {
        return license;
    }

    public boolean cachedResults() {
        return cacheResults;
    }

    public String getKickString() {
        return kickMessage;
    }
    
    public String alertMessage() {
        return alertMsg;
    }
    
    public boolean alertToStaff() {
        return alertToStaff;
    }

    public boolean runCommands() {
        return commandsEnabled;
    }
    
    public List<String> commands() {
        return commands;
    }

    public boolean kickPlayersOnDetect() {
        return kickPlayers;
    }
    
    public List<String> getPrefixWhitelists() {
        return prefixWhitelists;
    }
    
    public boolean isDatabaseEnabled() {
        return databaseEnabled;
    }

    public boolean useDatabaseCreds() {
        return useCredentials;
    }

    public String mongoDatabaseURL() {
        return mongoURL;
    }
    
    public String getDatabaseType() {
        return databaseType;
    }
    
    public String getDatabaseName() {
        return databaseName;
    }

    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }

    public String getIp() {
        return ip;
    }

    
    public List<String> allowedCountries() {
        return allowedCountries;
    }

    
    public List<String> blockedCountries() {
        return blockedCountries;
    }

    
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
