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
@MavenLibrary(groupId = "com.h2database", artifactId ="h2", version = "2.1.214")
@MavenLibrary(groupId = "org.mongodb", artifactId = "mongo-java-driver", version = "3.12.11")
@MavenLibrary(groupId = "mysql", artifactId = "mysql-connector-java", version = "8.0.30")
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
                configFile.getParentFile().mkdirs();
                MiscUtils.copy(INSTANCE.getResource( "config.yml"), configFile);
            }
            INSTANCE.config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        INSTANCE.vpnConfig = new VPNConfig();

        INSTANCE.executor.registerListeners();
        INSTANCE.vpnConfig.update();

        INSTANCE.messageHandler = new MessageHandler();

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
        executor.shutdown();
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
            e.printStackTrace();
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
