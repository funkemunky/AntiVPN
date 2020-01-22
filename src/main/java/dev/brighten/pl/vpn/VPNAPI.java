package dev.brighten.pl.vpn;

import cc.funkemunky.api.utils.MiscUtils;
import dev.brighten.db.db.Database;
import dev.brighten.db.db.FlatfileDatabase;
import dev.brighten.db.db.StructureSet;
import dev.brighten.db.utils.json.JSONException;
import dev.brighten.db.utils.json.JSONObject;
import dev.brighten.db.utils.json.JsonReader;
import dev.brighten.pl.utils.Config;
import lombok.val;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VPNAPI {

    public Database database;
    public ExecutorService vpnThread;

    public VPNAPI() {
        MiscUtils.printToConsole("&cLoading VPNHandler&7...");
        MiscUtils.printToConsole("&7Setting up Carbon database &eVPN-Cache&7...");
        database = new FlatfileDatabase("VPN-Cache");
        MiscUtils.printToConsole("&7Registering listener...");
        vpnThread = Executors.newFixedThreadPool(2);
    }

    public VPNResponse getResponse(Player player) {
        return getResponse(player.getAddress().getAddress().getHostAddress());
    }

    public void cacheReponse(VPNResponse response) {
        if(response.isSuccess()) {
            try {
                //Removing old value if it contains it.
                if(database.contains(response.getIp())) database.remove(response.getIp());

                val json = response.toJson();

                StructureSet set = database.create(response.getIp());

                for (String key : json.keySet()) {
                    set.input(key,  json.get(key));
                }

                set.save(database);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public VPNResponse getIfCached(String ipAddress) {
        if(database.contains(ipAddress)) {
            val list = database.get(ipAddress);

            if(list.size() > 0)
            return VPNResponse.fromSet(database.get(ipAddress).get(0));
        }
        
        return null;
    }

    public VPNResponse getResponse(String ipAddress) {
        try {

            val response = getIfCached(ipAddress);

            if(response != null) return response;

            String url = "https://funkemunky.cc/vpn?license="
                    + (Config.license.length() == 0 ? "none" : Config.license) + "&ip=" + ipAddress;

            JSONObject object = JsonReader.readJsonFromUrl(url);

            val toCacheAndReturn = VPNResponse.fromJson(object.toString());

            cacheReponse(toCacheAndReturn);

            return toCacheAndReturn;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}