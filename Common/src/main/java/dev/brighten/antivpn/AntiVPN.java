package dev.brighten.antivpn;

import dev.brighten.antivpn.api.PlayerExecutor;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.impl.AntiVPNCommand;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.local.H2VPN;
import dev.brighten.antivpn.database.mongo.MongoVPN;
import dev.brighten.antivpn.database.sql.MySqlVPN;
import dev.brighten.antivpn.depends.LibraryLoader;
import dev.brighten.antivpn.depends.MavenLibrary;
import dev.brighten.antivpn.depends.Relocate;
import dev.brighten.antivpn.message.MessageHandler;
import dev.brighten.antivpn.utils.ConfigDefault;
import dev.brighten.antivpn.utils.MiscUtils;
import dev.brighten.antivpn.utils.config.Configuration;
import dev.brighten.antivpn.utils.config.ConfigurationProvider;
import dev.brighten.antivpn.utils.config.YamlConfiguration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter(AccessLevel.PRIVATE)
@MavenLibrary(groupId = "com.h2database", artifactId ="h2", version = "2.2.220", relocations = {
        @Relocate(from ="org" + ".\\h2", to ="dev.brighten.antivpn.shaded.org.h2")})
@MavenLibrary(groupId = "org.mongodb", artifactId = "mongo-java-driver", version = "3.12.14", relocations = {
        @Relocate(from = "com." + "\\mongodb", to = "dev.brighten.antivpn.shaded.com.mongodb"),
        @Relocate(from = "org" + "\\.bson", to = "dev.brighten.antivpn.shaded.org.bson")
})
@MavenLibrary(
        groupId = "com.mysql",
        artifactId = "mysql-connector-j",
        version = "9.1.0",
        relocations = {
                @Relocate(from = "com.my\\" + "sql.cj", to = "dev.brighten.antivpn.shaded.com.mysql.cj"),
                @Relocate(from = "com.my\\" + "sql.jdbc", to = "dev.brighten.antivpn.shaded.com.mysql.jdbc")
        }
)
@MavenLibrary(groupId = "com.\\github\\.ben-manes\\.caffeine", artifactId = "caffeine", version = "3.1.8",
        relocations = {
                @Relocate(from = "com\\.github\\.benmanes\\.caffeine", to = "dev.brighten.antivpn.shaded.com.github.benmanes.caffeine"),
        })
public class AntiVPN {

    private static AntiVPN INSTANCE;
    private VPNConfig vpnConfig;
    private VPNExecutor executor;
    private PlayerExecutor playerExecutor;
    private VPNDatabase database;
    private MessageHandler messageHandler;
    private Configuration config;
    private List<Command> commands = new ArrayList<>();
    public int detections, checked;
    private File pluginFolder;

    public static void start(VPNExecutor executor, PlayerExecutor playerExecutor, File pluginFolder) {
        //Initializing

        INSTANCE = new AntiVPN();

        INSTANCE.pluginFolder = pluginFolder;
        INSTANCE.executor = executor;
        INSTANCE.playerExecutor = playerExecutor;

        LibraryLoader.loadAll(INSTANCE);

        try {
            File configFile = new File(pluginFolder, "config.yml");
            if(!configFile.exists()){
                if(configFile.getParentFile().mkdirs()) {
                    AntiVPN.getInstance().getExecutor().log("Created plugin folder!");
                }
                MiscUtils.copy(INSTANCE.getResource( "config.yml"), configFile);
            }
            INSTANCE.config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(configFile);
        } catch (IOException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not load config.yml, plugin disabling...", e);
            executor.disablePlugin();
            return;
        }

        INSTANCE.vpnConfig = new VPNConfig();

        INSTANCE.executor.registerListeners();
        INSTANCE.vpnConfig.update();

        INSTANCE.messageHandler = new MessageHandler();

        try {
            switch(INSTANCE.vpnConfig.getDatabaseType().toLowerCase()) {
                case "h2":
                case "local":
                case "flatfile": {
                    AntiVPN.getInstance().getExecutor().log("Using databaseType H2...");
                    INSTANCE.database = new H2VPN();
                    INSTANCE.database.init();
                    break;
                }
                case "mysql":
                case "sql":{
                    AntiVPN.getInstance().getExecutor().log("Using databaseType MySQL...");
                    INSTANCE.database = new MySqlVPN();
                    INSTANCE.database.init();
                    break;
                }
                case "mongo":
                case "mongodb":
                case "mongod": {
                    INSTANCE.database = new MongoVPN();
                    INSTANCE.database.init();
                    break;
                }
                default: {
                    AntiVPN.getInstance().getExecutor().log("Could not find database type \"" + INSTANCE.vpnConfig.getDatabaseType() + "\". " +
                            "Options: [MySQL]");
                    break;
                }
            }
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().logException("Could not initialize database, plugin disabling...", e);
            executor.disablePlugin();
            return;
        }

        //Registering commands
        INSTANCE.registerCommands();

        //Turning on alerts of players who are already online.
        playerExecutor.getOnlinePlayers().forEach(player -> {
            //We want to make sure they even have permission to see alerts before we make a bunch
            //of unnecessary database queries.
            if(player.hasPermission("antivpn.command.alerts")) {
                //Running database check for enabled alerts.
                INSTANCE.database.alertsState(player.getUuid(), player::setAlertsEnabled);
            }
        });

        AntiVPN.getInstance().getMessageHandler().initStrings(vpnString -> new ConfigDefault<>
                (vpnString.getDefaultMessage(), "messages." + vpnString.getKey(), AntiVPN.getInstance())
                .get());
        AntiVPN.getInstance().getMessageHandler().reloadStrings();

        // Starting kick checks
        AntiVPN.getInstance().getExecutor().startKickChecks();
    }

    public InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        } else {
            try {
                URL url = executor.getClass().getClassLoader().getResource(filename);
                if (url == null) {
                    return null;
                } else {
                    URLConnection connection = url.openConnection();
                    connection.setUseCaches(false);
                    return connection.getInputStream();
                }
            } catch (IOException var4) {
                return null;
            }
        }
    }

    public void stop() {
        if (database instanceof H2VPN) {
            database.shutdown();

            // Try to deregister driver
            try {
                java.sql.Driver driver = java.sql.DriverManager.getDriver("jdbc:h2:");
                if (driver != null) {
                    java.sql.DriverManager.deregisterDriver(driver);
                }
            } catch (Exception e) {
                // Log but don't throw
                executor.log("Failed to deregister H2 driver: " + e.getMessage());
            }
        }
        VPNExecutor.threadExecutor.shutdown();
        if(database != null) database.shutdown();
    }

    public void reloadDatabase() {
        database.shutdown();

        switch(AntiVPN.getInstance().getVpnConfig().getDatabaseType().toLowerCase()) {
            case "h2":
            case "local":
            case "flatfile": {
                AntiVPN.getInstance().getExecutor().log("Using databaseType H2...");
                INSTANCE.database = new H2VPN();
                INSTANCE.database.init();
                break;
            }
            case "mysql":
            case "sql":{
                AntiVPN.getInstance().getExecutor().log("Using databaseType MySQL...");
                INSTANCE.database = new MySqlVPN();
                INSTANCE.database.init();
                break;
            }
            case "mongo":
            case "mongodb":
            case "mongod": {
                INSTANCE.database = new MongoVPN();
                INSTANCE.database.init();
                break;
            }
            default: {
                AntiVPN.getInstance().getExecutor().log("Could not find database type \"" + INSTANCE.vpnConfig.getDatabaseType() + "\". " +
                        "Options: [MySQL]");
                break;
            }
        }
    }

    public static AntiVPN getInstance() {
        assert INSTANCE != null: "AntiVPN has not been initialized!";

        return INSTANCE;
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .save(getConfig(), new File(pluginFolder.getPath() + File.separator + "config.yml"));
        } catch (IOException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    public void reloadConfig() {
        try {

            config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new File(pluginFolder.getPath() + File.separator + "config.yml"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerCommands() {
        commands.add(new AntiVPNCommand());
    }
}
