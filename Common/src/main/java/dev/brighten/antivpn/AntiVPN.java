package dev.brighten.antivpn;

import dev.brighten.antivpn.api.PlayerExecutor;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.impl.AntiVPNCommand;
import dev.brighten.antivpn.database.Database;
import dev.brighten.antivpn.database.mongodb.MongoDatabase;
import dev.brighten.antivpn.database.postgres.PostgresDatabase;
import dev.brighten.antivpn.database.sqllite.LiteDatabase;
import dev.brighten.antivpn.database.sqllite.version.Version;
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
@MavenLibrary(groupId = "org\\.sqlite", artifactId ="sqlite-jdbc", version = "3.48.0.0", relocations = {
        @Relocate(from ="org" + ".\\sqlite", to ="dev.brighten.antivpn.shaded.org.sqlite")})
@MavenLibrary(groupId = "com.\\github\\.ben-manes\\.caffeine", artifactId = "caffeine", version = "3.1.8",
        relocations = {
                @Relocate(from = "com\\.github\\.benmanes\\.caffeine", to = "dev.brighten.antivpn.shaded.com.github.benmanes.caffeine"),
        })
@MavenLibrary(groupId = "org\\.postgresql", artifactId = "postgresql", version = "42.7.6",
        relocations = {
                @Relocate(from = "org\\.postgresql", to = "dev.brighten.antivpn.shaded.org.postgresql")
        })
@MavenLibrary(groupId = "com\\.mongodb", artifactId = "driver-sync", version = "5.5.0",
        relocations = {
                @Relocate(from = "com\\.mongodb.client", to = "dev.brighten.antivpn.shaded.com.mongodb.client")
        })
public class AntiVPN {

    private static AntiVPN INSTANCE;
    private VPNConfig vpnConfig;
    private VPNExecutor executor;
    private PlayerExecutor playerExecutor;
    private Database database;
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

        Version.register();

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

        INSTANCE.database = switch (INSTANCE.vpnConfig.getDatabaseType().toLowerCase()) {
            case "sqlite", "sqllite" -> new LiteDatabase();
            case "postgresql", "postgres" -> new PostgresDatabase();
            case "mongo", "mongodb" -> new MongoDatabase();
            default ->
                    throw new IllegalStateException("Unexpected database type set at config.yml 'database.type': \""
                            + INSTANCE.vpnConfig.getDatabaseType().toLowerCase() + "\"!" +
                            "Available types: 'sqlite', 'postgresql', 'mongodb'");
        };
        INSTANCE.database.init();

        //Registering commands
        INSTANCE.registerCommands();

        //Turning on alerts of players who are already online.
        playerExecutor.getOnlinePlayers().forEach(player -> {
            //We want to make sure they even have permission to see alerts before we make a bunch
            //of unnecessary database queries.
            if(player.hasPermission("antivpn.command.alerts")) {
                //Running database check for enabled alerts.
                INSTANCE.database.updateAlertsState(player.getUuid(), true);
                player.setAlertsEnabled(true);
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
        executor.shutdown();
        if(database != null) database.shutdown();
    }

    public void reloadDatabase() {
        database.shutdown();

        INSTANCE.database = new LiteDatabase();
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
