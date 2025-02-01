package dev.brighten.antivpn;

import dev.brighten.antivpn.api.PlayerExecutor;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.impl.AntiVPNCommand;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sqllite.LiteDatabase;
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

        INSTANCE.database = new LiteDatabase();
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
