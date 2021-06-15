package dev.brighten.pl.vpn;

import cc.funkemunky.api.utils.RunUtils;
import dev.brighten.db.utils.json.JSONException;
import dev.brighten.db.utils.json.JSONObject;
import dev.brighten.db.utils.json.JsonReader;
import dev.brighten.pl.config.Config;
import lombok.val;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VPNAPI {

    public VPNResponse getResponse(Player player) {
        return getResponse(player.getAddress().getAddress().getHostAddress());
    }

    private final Map<String, VPNResponse> cache = new ConcurrentHashMap<>();

    public VPNAPI() {
        RunUtils.taskTimerAsync(cache::clear, 24000, 24000); //Clear cache every 20 minutes
    }

    public void cacheReponse(VPNResponse response) {
        cache.put(response.getIp(), response);
    }

    public VPNResponse getIfCached(String ipAddress) {
        return cache.getOrDefault(ipAddress, null);
    }

    public VPNResponse getResponse(String ipAddress) {
        try {

            val response = getIfCached(ipAddress);

            if(response != null) return response;

            String url = "https://funkemunky.cc/vpn?license="
                    + (Config.license.length() == 0 ? "none" : Config.license) + "&ip=" + ipAddress;

            JSONObject object = JsonReader.readJsonFromUrl(url);

            if(object.has("success") && object.getBoolean("success")) {
                val toCacheAndReturn = VPNResponse.fromJson(object.toString());

                cacheReponse(toCacheAndReturn);

                return toCacheAndReturn;
            } else System.out.println("failed");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}