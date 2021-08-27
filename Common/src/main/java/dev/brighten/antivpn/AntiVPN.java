package dev.brighten.antivpn;

import dev.brighten.antivpn.api.PlayerExecutor;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.impl.AntiVPNCommand;
import dev.brighten.antivpn.command.impl.LookupCommand;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sql.MySqlVPN;
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

@Getter
@Setter(AccessLevel.PRIVATE)
public class AntiVPN {

    private static AntiVPN INSTANCE;
    private VPNConfig config;
    private VPNExecutor executor;
    private PlayerExecutor playerExecutor;
    private VPNDatabase database;
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

        switch(INSTANCE.config.getDatabaseType().toLowerCase()) {
            case "mysql":
            case "sql":{
                System.out.println("Using databaseType MySQL...");
                INSTANCE.database = new MySqlVPN();
                INSTANCE.database.init();
                break;
            }
            case "mongo":
            case "mongodb":
            case "mongod": {
                System.out.println("We currently do not support Mongo, but this is coming in future updates.");
                break;
            }
            default: {
                System.out.println("Could not find database type \"" + INSTANCE.config.getDatabaseType() + "\". " +
                        "Options: [MySQL]");
                break;
            }
        }

        //Registering commands
        INSTANCE.registerCommands();
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
