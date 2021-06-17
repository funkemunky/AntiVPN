package dev.brighten.antivpn;

import dev.brighten.antivpn.api.PlayerExecutor;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.command.Command;
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
    private List<Command> commands = new ArrayList<>();

    public static void start(VPNConfig config, VPNExecutor executor, PlayerExecutor playerExecutor) {
        //Initializing

        INSTANCE = new AntiVPN();

        INSTANCE.config = config;
        INSTANCE.executor = executor;
        INSTANCE.playerExecutor = playerExecutor;

        INSTANCE.executor.registerListeners();
        INSTANCE.config.update();

        //Registering commands
        registerCommands();
    }

    public void stop() {
        executor.shutdown();
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

    private static void registerCommands() {

    }
}
