package dev.brighten.antivpn.api;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.ConfigDefault;

import java.util.ArrayList;
import java.util.Arrays;
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
            defaultWhitelistCountries = new ConfigDefault<>(true, "countries.whitelist",
                    AntiVPN.getInstance()),
            defaultMetrics = new ConfigDefault<>(true, "bstats", AntiVPN.getInstance());
    private final ConfigDefault<Integer>
            defaultPort = new ConfigDefault<>(-1, "database.port", AntiVPN.getInstance());
    private final ConfigDefault<List<String>> prefixWhitelistsDefault = new ConfigDefault<>(new ArrayList<>(),
            "prefixWhitelists", AntiVPN.getInstance()), defaultCommands = new ConfigDefault<>(
            Collections.singletonList("kick %player% VPNs are not allowed on our server!"), "commands.execute",
            AntiVPN.getInstance()),
            defCountryKickCommands = new ConfigDefault<>(Collections.singletonList(
                    "kick %player% &cSorry, but our server does not allow connections from\\n&f%country%"),
                    "countries.commands", AntiVPN.getInstance()),
            defCountrylist = new ConfigDefault<>(new ArrayList<>(), "countries.list",
                    AntiVPN.getInstance());

    private String license, kickMessage, databaseType, databaseName, mongoURL, username, password, ip, alertMsg;
    private List<String> prefixWhitelists, commands, countryList, countryKickCommands;
    private int port;
    private boolean cacheResults, databaseEnabled, useCredentials, commandsEnabled, kickPlayers, alertToStaff,
            metrics, whitelistCountries;

    /**
     * License from https://funkemunky.cc/shop to be used for more queries.
     * @return String
     */
    public String getLicense() {
        return license;
    }

    /**
     * If true, results will be cached to reduce queries to https://funkemunky.cc
     * @return boolean
     */
    public boolean cachedResults() {
        return cacheResults;
    }

    /**
     * Will be used for vanilla kick message when {@link VPNConfig#runCommands()} is true.
     * @return String
     */
    public String getKickString() {
        return kickMessage;
    }

    /**
     * Message to send staff on proxy detection.
     * @return String
     */
    public String alertMessage() {
        return alertMsg;
    }

    /**
     * If true, staff will be alerted on proxy detection.
     * @return boolean
     */
    public boolean alertToStaff() {
        return alertToStaff;
    }

    /**
     * If true, will run {@link VPNConfig#commands()} on detect. If not, it will use vanilla kicking methods.
     * @return boolean
     */
    public boolean runCommands() {
        return commandsEnabled;
    }

    /**
     * Commands to run on proxy detection.
     * @return List
     */
    public List<String> commands() {
        return commands;
    }

    /**
     * If false, no commands nor kick will be run on proxy detection.
     * @return boolean
     */
    public boolean kickPlayersOnDetect() {
        return kickPlayers;
    }

    /**
     * Returns Strings of which are checked against the beginning of player names. Used to
     * allow Geyser-connected players to join.
     * @return List
     */
    public List<String> getPrefixWhitelists() {
        return prefixWhitelists;
    }

    /**
     * Returns true if we want to use a database
     * @return boolean
     */
    public boolean isDatabaseEnabled() {
        return databaseEnabled;
    }

    /**
     * Whether or not the database we want to connect to requires credentials.
     * @return boolean
     */
    public boolean useDatabaseCreds() {
        return useCredentials;
    }

    /**
     * Only for Mongo only. URL used for connecting to database. Overrides other fields
     * @return String
     */
    public String mongoDatabaseURL() {
        return mongoURL;
    }

    /**
     * Database type. Either MySQL and Mongo.
     * @return String
     */
    public String getDatabaseType() {
        return databaseType;
    }

    /**
     * Database name
     * @return String
     */
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Database username
     * @return String
     */
    public String getUsername() {
        return username;
    }

    /**
     * Database Password
     * @return String
     */
    public String getPassword() {
        return password;
    }

    /**
     * Database IP
     * @return String
     */
    public String getIp() {
        return ip;
    }

    /**
     * Returns the list of ISO country codes we need to check.
     * @return List
     */
    public List<String> countryList() {
        return countryList;
    }

    /**
     * If true, we only allow the {@link VPNConfig#countryKickCommands()}. If false, we blacklist them.
     * @return boolean
     */
    public boolean whitelistCountries() {
        return whitelistCountries;
    }

    /**
     * Returns our configured commands to run on player country detection.
     * @return List
     */
    public List<String> countryKickCommands() {
        return countryKickCommands;
    }

    /**
     * Gets the port based on configuration. If {@link VPNConfig#port} is -1, will get default port
     * based on {@link VPNConfig#getDatabaseType()} lowerCase().
     * @return int
     */
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


    /**
     * If true, https://bstats.org metrics will be collected to improve KauriVPN.
     * @return boolean
     */
    public boolean metrics() {
        return metrics;
    }

    /**
     * Grabs all information from the config.yml
     */
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
        countryList = defCountrylist.get();
        whitelistCountries = defaultWhitelistCountries.get();
        countryKickCommands = defCountryKickCommands.get();
    }

}
