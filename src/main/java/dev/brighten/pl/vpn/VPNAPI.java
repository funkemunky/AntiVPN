package dev.brighten.pl.vpn;

import cc.funkemunky.api.utils.MiscUtils;
import dev.brighten.db.db.*;
import dev.brighten.db.utils.json.JSONException;
import dev.brighten.db.utils.json.JSONObject;
import dev.brighten.db.utils.json.JsonReader;
import dev.brighten.db.utils.security.hash.Hash;
import dev.brighten.db.utils.security.hash.HashType;
import dev.brighten.pl.config.Config;
import dev.brighten.pl.config.FlatfileConfig;
import dev.brighten.pl.config.MongoConfig;
import dev.brighten.pl.config.MySQLConfig;
import lombok.val;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class VPNAPI {

    public Database database;

    public Hash hashAlgorithm;

    public VPNAPI() {
        MiscUtils.printToConsole("&cLoading VPNHandler&7...");
        MiscUtils.printToConsole("&7Setting up Carbon database...");
        if(FlatfileConfig.enabled) {
            setupFlatfile();
        } else if(MySQLConfig.enabled && MongoConfig.enabled) {
            MiscUtils.printToConsole("&7Both MySQL and Mongo enabled! Defaulting to Flatfile.");
            setupFlatfile();
        } else if(MongoConfig.enabled) {
            MiscUtils.printToConsole("&7Setting up Mongo database &f" + MongoConfig.database + "&7...");
            database = new MongoDatabase(MongoConfig.database);

            if(MongoConfig.username.length() > 0) {
                val authDB =  MongoConfig.authDatabase.length() > 0 ? MongoConfig.authDatabase : MongoConfig.database;

                MiscUtils.printToConsole("&7Connecting to database with credentials to " + authDB + "...");
                database.connect(MongoConfig.ip, String.valueOf(MongoConfig.port),
                        authDB, MongoConfig.username, MongoConfig.password, MongoConfig.database);
            } else {
                MiscUtils.printToConsole("&7Connecting to database without credentials...");
                database.connect(MongoConfig.ip, String.valueOf(MongoConfig.port), MongoConfig.database);
            }
        } else if(MySQLConfig.enabled) {
            database = new MySQLDatabase(MySQLConfig.database);

            MiscUtils.printToConsole("&7Connecting to MySQL database...");
            database.connect(MySQLConfig.ip, String.valueOf(MySQLConfig.port), MySQLConfig.database,
                    String.valueOf(MySQLConfig.ssl), MySQLConfig.username, MySQLConfig.password);
        } else {
            MiscUtils.printToConsole("&7No database enabled! No caching will occur.");
        }

        if(database != null) {
            MiscUtils.printToConsole("&7Loading database mappings...");
            database.loadMappings();
        }

        MiscUtils.printToConsole("&7Checking hash algorithm " + Config.hashType + "...");
        if(Config.hashIp) {
            Hash.loadHashes();
            hashAlgorithm = Hash.getHashByType(Arrays.stream(HashType.values())
                    .filter(type -> type.name().equalsIgnoreCase(Config.hashType)).findFirst().orElse(HashType.SHA1));
            MiscUtils.printToConsole("&7Using Hash algorithm &f" + hashAlgorithm.hashType.name() + "&7.");
        } else MiscUtils.printToConsole("&7Hashing is not enabled.");
    }

    public VPNResponse getResponse(Player player) {
        return getResponse(player.getAddress().getAddress().getHostAddress());
    }

    public void cacheReponse(VPNResponse response) {
        if(database != null && response.isSuccess()) {
            try {
                //Removing old value if it contains it.
                if(database.contains(response.getIp())) database.remove(response.getIp());

                val json = response.toJson();

                if(hashAlgorithm != null && json.has("ip")) {
                    json.put("ip", hashAlgorithm.hash(json.getString("ip")));
                }

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
        if(database != null && database.contains(ipAddress)) {
            val list = hashAlgorithm != null
                    ? database.get(set -> hashAlgorithm.hashEqualsKey(set.getObject("ip"), ipAddress))
                    : database.get(ipAddress);

            if(list.size() > 0) {
                long timeStamp = System.currentTimeMillis();
                if(list.size() == 1) {
                    val response = VPNResponse.fromSet(database.get(ipAddress).get(0));

                    if(timeStamp - response.getCacheTime() < TimeUnit.DAYS.toMillis(7)) {
                        return response;
                    } else {
                        database.remove(response.getIp());
                    }
                } else {;
                    for (StructureSet set : list) {
                        if(!set.contains("cacheTime")){
                            set.input("cacheTime", timeStamp);
                            continue;
                        }
                        long cacheTime = set.getObject("cacheTime");
                        if(timeStamp - cacheTime > TimeUnit.DAYS.toMillis(7)) {
                            database.remove(set.getId());
                            list.remove(set);
                        }
                    }

                    if(list.size() > 0) {
                        val ip = list.stream()
                                .max(Comparator.comparing(set -> (long)set.getObject("cacheTime"))).get();

                        return VPNResponse.fromSet(ip);
                    }
                }
            }
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

    private void setupFlatfile() {
        MiscUtils.printToConsole("&7Setting up Flatfile Database &f" + FlatfileConfig.database + "&7...");
        database = new FlatfileDatabase(FlatfileConfig.database);
    }
}