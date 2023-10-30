package dev.brighten.antivpn.database.mongo;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.UpdateOptions;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.web.objects.VPNResponse;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MongoVPN implements VPNDatabase {

    private MongoCollection<Document> settingsDocument, cacheDocument;
    private MongoClient client;
    private MongoDatabase antivpnDatabase;

    @Override
    public Optional<VPNResponse> getStoredResponse(String ip) {
        Document rdoc = cacheDocument.find(Filters.eq("ip", ip)).first();

        if(rdoc != null) {
            long lastUpdate = rdoc.get("lastAccess", 0L);

            if(System.currentTimeMillis() - lastUpdate > TimeUnit.HOURS.toMillis(1)) {
                VPNExecutor.threadExecutor.execute(() -> deleteResponse(ip));
                return Optional.empty();
            }

            return Optional.of(VPNResponse.builder().asn(rdoc.getString("asn")).ip(ip)
                    .countryName(rdoc.getString("countryName"))
                    .countryCode(rdoc.getString("countryCode"))
                    .city(rdoc.getString("city"))
                    .isp(rdoc.getString("isp"))
                    .method(rdoc.getString("method"))
                    .timeZone(rdoc.getString("timeZone"))
                    .proxy(rdoc.getBoolean("proxy"))
                    .cached(rdoc.getBoolean("cached"))
                    .success(true)
                    .latitude(rdoc.getDouble("latitude"))
                    .longitude(rdoc.getDouble("longitude"))
                    .lastAccess(rdoc.get("lastAccess", 0L))
                    .build());
        }
        return Optional.empty();
    }

    @Override
    public void cacheResponse(VPNResponse toCache) {
        Document rdoc = new Document("ip", toCache.getIp());

        rdoc.put("asn", toCache.getAsn());
        rdoc.put("countryName", toCache.getCountryName());
        rdoc.put("countryCode", toCache.getCountryCode());
        rdoc.put("city", toCache.getCity());
        rdoc.put("isp", toCache.getIsp());
        rdoc.put("method", toCache.getMethod());
        rdoc.put("timeZone", toCache.getTimeZone());
        rdoc.put("proxy", toCache.isProxy());
        rdoc.put("cached", toCache.isCached());
        rdoc.put("success", toCache.isSuccess());
        rdoc.put("latitude", toCache.getLatitude());
        rdoc.put("longitude", toCache.getLongitude());
        rdoc.put("lastAccess", System.currentTimeMillis());

        VPNExecutor.threadExecutor.execute(() -> {
            Bson update = new Document("$set", rdoc);
            cacheDocument.updateOne(Filters.eq("ip", toCache.getIp()), update,
                    new UpdateOptions().upsert(true));
        });
    }

    @Override
    public void deleteResponse(String ip) {
        cacheDocument.deleteMany(Filters.eq("ip", ip));
    }

    @Override
    public boolean isWhitelisted(UUID uuid) {
        return settingsDocument
                .find(Filters.and(Filters.eq("setting", "whitelist"),
                        Filters.eq("uuid", uuid.toString()))).first() != null;
    }

    @Override
    public boolean isWhitelisted(String ip) {
        return settingsDocument
                .find(Filters.and(Filters.eq("setting", "whitelist"),
                        Filters.eq("ip", ip))).first() != null;
    }

    @Override
    public void setWhitelisted(UUID uuid, boolean whitelisted) {
        if(whitelisted) {
            Document wdoc = new Document("setting", "whitelist");
            wdoc.put("uuid", uuid.toString());
            AntiVPN.getInstance().getExecutor().getWhitelisted().add(uuid);
            VPNExecutor.threadExecutor.execute(() -> settingsDocument.insertOne(wdoc));
        } else {
            AntiVPN.getInstance().getExecutor().getWhitelisted().remove(uuid);
            VPNExecutor.threadExecutor.execute(() -> settingsDocument.deleteMany(Filters
                    .and(
                            Filters.eq("setting", "whitelist"),
                            Filters.eq("uuid", uuid.toString()))));
        }
    }

    @Override
    public void setWhitelisted(String ip, boolean whitelisted) {
        if(whitelisted) {
            Document wdoc = new Document("setting", "whitelist").append("ip", ip);

            AntiVPN.getInstance().getExecutor().getWhitelistedIps().add(ip);
            VPNExecutor.threadExecutor.execute(() -> settingsDocument.insertOne(wdoc));
        } else {
            AntiVPN.getInstance().getExecutor().getWhitelistedIps().remove(ip);
            VPNExecutor.threadExecutor.execute(() -> settingsDocument.deleteMany(Filters
                    .and(
                            Filters.eq("setting", "whitelist"),
                            Filters.eq("ip", ip))));
        }
    }

    @Override
    public List<UUID> getAllWhitelisted() {
        List<UUID> uuids = new ArrayList<>();
        settingsDocument.find(Filters.and(Filters.eq("setting", "whitelist"),
                Filters.exists("uuid")))
                .forEach((Consumer<? super Document>) doc -> uuids.add(UUID.fromString(doc.getString("uuid"))));
        return uuids;
    }

    @Override
    public List<String> getAllWhitelistedIps() {
        List<String> ips = new ArrayList<>();
        settingsDocument.find(Filters.and(Filters.eq("setting", "whitelist"),
                        Filters.exists("ip")))
                .forEach((Consumer<? super Document>) doc -> ips.add(doc.getString("ip")));
        return ips;
    }

    @Override
    public void getStoredResponseAsync(String ip, Consumer<Optional<VPNResponse>> result) {
        VPNExecutor.threadExecutor.execute(() -> result.accept(getStoredResponse(ip)));
    }

    @Override
    public void isWhitelistedAsync(UUID uuid, Consumer<Boolean> result) {
        VPNExecutor.threadExecutor.execute(() -> result.accept(isWhitelisted(uuid)));
    }

    @Override
    public void isWhitelistedAsync(String ip, Consumer<Boolean> result) {
        VPNExecutor.threadExecutor.execute(() -> result.accept(isWhitelisted(ip)));
    }

    @Override
    public void alertsState(UUID uuid, Consumer<Boolean> result) {
        VPNExecutor.threadExecutor.execute(() -> result.accept(settingsDocument
                .find(Filters.and(Filters.eq("setting", "alerts"),
                Filters.eq("uuid", uuid.toString()))).first() != null));
    }

    @Override
    public void updateAlertsState(UUID uuid, boolean state) {
        VPNExecutor.threadExecutor.execute(() -> {
            settingsDocument.deleteMany(Filters.and(Filters.eq("setting", "alerts"),
                    Filters.eq("uuid", uuid.toString())));
            if(state) {
                Document adoc = new Document("setting", "alerts");

                adoc.put("uuid", uuid.toString());
                settingsDocument.insertOne(adoc);
            }
        });
    }

    @Override
    public void clearResponses() {
        cacheDocument.deleteMany(Filters.exists("ip"));
    }

    @Override
    public void init() {
        if(AntiVPN.getInstance().getVpnConfig().mongoDatabaseURL().length() > 0) { //URL
            ConnectionString cs = new ConnectionString(AntiVPN.getInstance().getVpnConfig().mongoDatabaseURL());
            MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(cs).build();
            client = MongoClients.create(settings);
        } else {
            MongoClientSettings.Builder settingsBld = MongoClientSettings.builder().readPreference(ReadPreference.nearest())
                    .applyToClusterSettings(builder -> {
                        builder.hosts(Collections.singletonList(new ServerAddress(AntiVPN.getInstance().getVpnConfig().getIp(),
                                AntiVPN.getInstance().getVpnConfig().getPort())));
                    });
            if(AntiVPN.getInstance().getVpnConfig().useDatabaseCreds()) {
                settingsBld = settingsBld.credential(MongoCredential
                        .createCredential(AntiVPN.getInstance().getVpnConfig().getUsername(),
                                AntiVPN.getInstance().getVpnConfig().getDatabaseName(),
                                AntiVPN.getInstance().getVpnConfig().getPassword().toCharArray()));
            }

            client = MongoClients.create(settingsBld.build());
        }
        antivpnDatabase = client.getDatabase(AntiVPN.getInstance().getVpnConfig().getDatabaseName());

        settingsDocument = antivpnDatabase.getCollection("settings");
        if(settingsDocument.listIndexes().first() == null) {
            AntiVPN.getInstance().getExecutor().log("Created index for settings collection!");
            settingsDocument.createIndex(Indexes.ascending("ip"));
        }
        cacheDocument = antivpnDatabase.getCollection("cache");
    }

    @Override
    public void shutdown() {
        settingsDocument = null;
        cacheDocument = null;
        client.close();
    }
}
