/*
 * Copyright 2026 Dawson Hessler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.brighten.antivpn.api;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.ConfigDefault;
import lombok.Getter;

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
            defaultCountryKickReason = new ConfigDefault<>(
                    "&cSorry, but our server does not allow connections from\n&f%country%",
                    "countries.vanillaKickReason", AntiVPN.getInstance()),
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
            defCountryKickCommands = new ConfigDefault<>(Collections.emptyList(),
                    "countries.commands", AntiVPN.getInstance()),
            defCountrylist = new ConfigDefault<>(new ArrayList<>(), "countries.list",
                    AntiVPN.getInstance());

    @Getter
    private String license;
    @Getter
    private String kickMessage;
    @Getter
    private String databaseType;
    @Getter
    private String databaseName;
    private String mongoURL;
    @Getter
    private String username;
    @Getter
    private String password;
    @Getter
    private String ip;
    @Getter
    private String alertMsg;
    @Getter
    private String countryVanillaKickReason;
    @Getter
    private List<String> prefixWhitelists;
    private List<String> commands;
    @Getter
    private List<String> countryList;
    private List<String> countryKickCommands;
    private int port;
    private boolean cacheResults;
    @Getter
    private boolean databaseEnabled;
    private boolean useCredentials;
    @Getter
    private boolean commandsEnabled;
    @Getter
    private boolean kickPlayers;
    private boolean alertToStaff;
    private boolean metrics;
    private boolean whitelistCountries;

    /**
     * If true, results will be cached to reduce queries to <a href="https://funkemunky.cc">...</a>
     * @return boolean
     */
    public boolean cachedResults() {
        return cacheResults;
    }

    /**
     * If true, staff will be alerted on proxy detection.
     * @return boolean
     */
    public boolean isAlertToSTaff() {
        return alertToStaff;
    }

    /**
     * Commands to run on proxy detection.
     * @return List
     */
    public List<String> commands() {
        return commands;
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
     * If true, we only allow the {@link VPNConfig#countryKickCommands()}. If false, we blacklist them.
     * @return boolean
     */
    public boolean getWhitelistCountries() {
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
     * If true, <a href="https://bstats.org">...</a> metrics will be collected to improve KauriVPN.
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
        countryVanillaKickReason = defaultCountryKickReason.get();
    }

}