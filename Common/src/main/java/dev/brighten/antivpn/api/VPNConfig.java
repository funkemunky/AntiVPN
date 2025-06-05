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
            defaultDatabaseType = new ConfigDefault<>("SQLite",
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

    /**
     * -- GETTER --
     *  License from <a href="https://funkemunky.cc/shop">...</a> to be used for more queries.
     *
     */
    @Getter
    private String license, kickMessage, databaseType, databaseName, mongoURL, username, password,
            ip, alertMsg, countryVanillaKickReason;
    @Getter
    private List<String> prefixWhitelists, commands, countryList, countryKickCommands;
    @Getter
    private int port;
    @Getter
    private boolean cacheResults, databaseEnabled, useCredentials, commandsEnabled, kickPlayers, alertToStaff,
            metrics, whitelistCountries;

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
