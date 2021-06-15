package dev.brighten.antivpn;

import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.utils.VPNResponse;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.utils.json.JSONObject;
import dev.brighten.antivpn.utils.json.JsonReader;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Getter
@Setter(AccessLevel.PRIVATE)
public class AntiVPN {

    private static AntiVPN INSTANCE;
    private VPNConfig config;
    private VPNExecutor executor;

    public static void start(VPNConfig config, VPNExecutor executor) {
        //Initializing

        INSTANCE = new AntiVPN();

        INSTANCE.setConfig(config);
        INSTANCE.setExecutor(executor);

        getInstance().getExecutor().registerListeners();
        getInstance().getConfig().update();
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

}
