package dev.brighten.pl.vpn;

import cc.funkemunky.api.utils.MathUtils;
import cc.funkemunky.api.utils.MiscUtils;
import cc.funkemunky.api.utils.RunUtils;
import cc.funkemunky.carbon.db.Database;
import cc.funkemunky.carbon.db.StructureSet;
import cc.funkemunky.carbon.db.flatfile.FlatfileDatabase;
import cc.funkemunky.carbon.utils.Pair;
import cc.funkemunky.carbon.utils.json.JSONException;
import cc.funkemunky.carbon.utils.json.JSONObject;
import dev.brighten.pl.AntiVPN;
import dev.brighten.pl.utils.Config;
import dev.brighten.pl.utils.JsonReader;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Objects;
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

        //Running saveDatabase task.
        MiscUtils.printToConsole("&7Running database saving task...");
        RunUtils.taskTimerAsync(database::saveDatabase, AntiVPN.INSTANCE, 0, 20 * 60 * 2);
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


                val pairs = json.keySet().stream().map(key -> {
                    try {
                        return new Pair<>(key,  json.get(key));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).filter(Objects::nonNull).toArray(Pair[]::new);

                StructureSet set = database.createStructure(response.getIp(), pairs);

                if(MathUtils.getDelta(set.getObjects().size(), pairs.length) > 1) {
                    MiscUtils.printToConsole("&cThere was an error saving response for IP &f"
                            + response.getIp() + "&c. &7Removing from database...");
                    database.remove(response.getIp());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public VPNResponse getIfCached(String ipAddress) {
        if(database.contains(ipAddress)) {
            return VPNResponse.fromSet(database.get(ipAddress));
        } else {
            return null;
        }
    }

    public VPNResponse getResponse(String ipAddress) {
        try {

            val response = getIfCached(ipAddress);

            if(response != null) return response;

            String url = "https://funkemunky.cc/vpn?license=" + Config.license + "&ip=" + ipAddress;

            JSONObject object = JsonReader.readJsonFromUrl(url);

            if (!object.has("ip")) {
                return null;
            }

            val toCacheAndReturn = VPNResponse.fromJson(object.toString());

            cacheReponse(toCacheAndReturn);

            return toCacheAndReturn;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}