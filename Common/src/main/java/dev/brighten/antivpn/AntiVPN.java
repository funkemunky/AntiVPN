package dev.brighten.antivpn;

import dev.brighten.antivpn.api.PlayerExecutor;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.impl.AntiVPNCommand;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sql.MySqlVPN;
import dev.brighten.antivpn.message.MessageHandler;
import dev.brighten.antivpn.utils.VPNResponse;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.utils.json.JSONObject;
import dev.brighten.antivpn.utils.json.JsonReader;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// For logging using getLogger if Bukkit;
import org.bukkit.Bukkit;

@Getter
@Setter(AccessLevel.PRIVATE)
public class AntiVPN {

    private static AntiVPN INSTANCE;
    private VPNConfig config;
    private VPNExecutor executor;
    private PlayerExecutor playerExecutor;
    private VPNDatabase database;
    private MessageHandler messageHandler;
    private List<Command> commands = new ArrayList<>();
    public int detections, checked;

    public static void start(VPNConfig config, VPNExecutor executor, PlayerExecutor playerExecutor) {
        //Initializing

        INSTANCE = new AntiVPN();

        INSTANCE.config = config;
        INSTANCE.executor = executor;
        INSTANCE.playerExecutor = playerExecutor;

        INSTANCE.executor.registerListeners();
        INSTANCE.config.update();

        INSTANCE.messageHandler = new MessageHandler();

        switch(INSTANCE.config.getDatabaseType().toLowerCase()) {
            case "mysql":
            case "sql":{
                if (INSTANCE.config.isBukkit())
                    Bukkit.getLogger().info("Using databaseType MySQL...");
                else
                    System.out.println("Using databaseType MySQL...");
                INSTANCE.database = new MySqlVPN();
                INSTANCE.database.init();
                break;
            }
            case "mongo":
            case "mongodb":
            case "mongod": {
                if (INSTANCE.config.isBukkit())
                    Bukkit.getLogger().info("We currently do not support Mongo, but this is coming in future updates.");
                else
                    System.out.println("We currently do not support Mongo, but this is coming in future updates.");
                break;
            }
            default: {
                if (INSTANCE.config.isBukkit())
                    Bukkit.getLogger().info("Could not find database type \"" + INSTANCE.config.getDatabaseType() + "\". " +
                    "Options: [MySQL]");
                else
                    System.out.println("Could not find database type \"" + INSTANCE.config.getDatabaseType() + "\". " +
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
    }

    public void stop() {
        executor.shutdown();
        if(database != null) database.shutdown();
    }

    public static AntiVPN getInstance() {
        assert INSTANCE != null: "AntiVPN has not been initialized!";

        return INSTANCE;
    }

    public static VPNResponse getVPNResponse(String ip, String license, boolean cachedResults /* faster if set to true*/)
            throws JSONException, IOException {
        JSONObject result = JsonReader.readJsonFromUrl(String
                .format("https://funkemunky.cc/vpn?ip=%s&license=%s&cache=%s",
                        ip, license.length() == 0 ? "none" : license, cachedResults));

        return VPNResponse.fromJson(result);
    }

    private void registerCommands() {
        commands.add(new AntiVPNCommand());
    }
}
