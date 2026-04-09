/*
 * Copyright 2026 Dawson Hessler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.brighten.antivpn.database.mongo;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.version.Version;
import dev.brighten.antivpn.utils.CIDRUtils;
import dev.brighten.antivpn.web.objects.VPNResponse;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MongoVPN implements VPNDatabase {

    public MongoCollection<Document> settingsDocument;
    MongoCollection<Document> cacheDocument;
    private MongoClient client;
    public MongoDatabase antivpnDatabase;

    public MongoVPN() {
        AntiVPN.getInstance().getExecutor().getThreadExecutor().scheduleAtFixedRate(() -> {
            if(!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled()) return;

            //Refreshing whitelisted players
            AntiVPN.getInstance().getExecutor().getWhitelisted().clear();
            AntiVPN.getInstance().getExecutor().getWhitelisted()
                    .addAll(AntiVPN.getInstance().getDatabase().getAllWhitelisted());

            //Refreshing whitlisted IPs
            AntiVPN.getInstance().getExecutor().getWhitelistedIps().clear();
            AntiVPN.getInstance().getExecutor().getWhitelistedIps()
                    .addAll(AntiVPN.getInstance().getDatabase().getAllWhitelistedIps());
        }, 2, 30, TimeUnit.SECONDS);
    }
    @Override
    public Optional<VPNResponse> getStoredResponse(String ip) {
        Document rdoc = cacheDocument.find(Filters.eq("ip", ip)).first();

        if(rdoc != null) {
            long lastUpdate = rdoc.get("lastAccess", 0L);

            if(System.currentTimeMillis() - lastUpdate > TimeUnit.HOURS.toMillis(1)) {
                AntiVPN.getInstance().getExecutor().getThreadExecutor().execute(() -> deleteResponse(ip));
                return null;
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
        if(AntiVPN.getInstance().getVpnConfig().cachedResults()) {
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

            AntiVPN.getInstance().getExecutor().getThreadExecutor().execute(() -> {
                Bson update = new Document("$set", rdoc);
                cacheDocument.updateOne(Filters.eq("ip", toCache.getIp()), update,
                        new UpdateOptions().upsert(true));
            });
        }
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
    public boolean isWhitelisted(String cidr) {
        try {
            return isWhitelisted(new CIDRUtils(cidr));
        } catch (UnknownHostException e) {
            AntiVPN.getInstance().getExecutor().log("Failed to check whitelist for IP: " + cidr, e);
            return false;
        }
    }

    @Override
    public boolean isWhitelisted(CIDRUtils cidr) {
        var start =  new Decimal128(new BigDecimal(cidr.getStartIpInt()));
        var end = new Decimal128(new BigDecimal(cidr.getEndIpInt()));
        return settingsDocument.find(Filters.and(Filters.eq("setting", "whitelist"),
                Filters.lte("ip_start", start), Filters.gte("ip_end", end))).first() != null;
    }

    @Override
    public void addWhitelist(UUID uuid) {
        Document wdoc = new Document("setting", "whitelist");
        wdoc.put("uuid", uuid.toString());
        AntiVPN.getInstance().getExecutor().getWhitelisted().add(uuid);
        settingsDocument.insertOne(wdoc);
    }

    @Override
    public void removeWhitelist(UUID uuid) {
        AntiVPN.getInstance().getExecutor().getWhitelisted().remove(uuid);
        settingsDocument.deleteMany(Filters
                .and(
                        Filters.eq("setting", "whitelist"),
                        Filters.eq("uuid", uuid.toString())));
    }

    @Override
    public void addWhitelist(CIDRUtils cidr) {
        Document doc = new Document("setting", "whitelist");
        doc.append("ip_start", new Decimal128(new BigDecimal(cidr.getStartIpInt())));
        doc.append("ip_end", new Decimal128(new BigDecimal(cidr.getEndIpInt())));
        doc.append("cidr_string", cidr.getCidr());

        settingsDocument.insertOne(doc);
    }

    @Override
    public void removeWhitelist(CIDRUtils cidr) {
        settingsDocument.deleteMany(Filters
                .and(
                        Filters.eq("setting", "whitelist"),
                        Filters.eq("ip_start", new Decimal128(new BigDecimal(cidr.getStartIpInt()))),
                        Filters.eq("ip_end", new Decimal128(new BigDecimal(cidr.getEndIpInt())))));
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
    public List<CIDRUtils> getAllWhitelistedIps() {
        List<CIDRUtils> ips = new ArrayList<>();
        settingsDocument.find(Filters.and(Filters.eq("setting", "whitelist"),
                        Filters.exists("cidr_string"))).forEach((Consumer<? super Document>) doc -> {
                    try {
                        var cidr = new CIDRUtils(doc.getString("cidr_string"));
                        ips.add(cidr);
                    } catch (UnknownHostException e) {
                        AntiVPN.getInstance().getExecutor().logException("Could not format ip " + doc.getString("cidr_string") + " into a CIDR!", e);
                    }
                });
        return ips;
    }

    @Override
    public void alertsState(UUID uuid, Consumer<Boolean> result) {
        AntiVPN.getInstance().getExecutor().getThreadExecutor().execute(() -> result.accept(settingsDocument
                .find(Filters.and(Filters.eq("setting", "alerts"),
                Filters.eq("uuid", uuid.toString()))).first() != null));
    }

    @Override
    public void updateAlertsState(UUID uuid, boolean state) {
        AntiVPN.getInstance().getExecutor().getThreadExecutor().execute(() -> {
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
        if(!AntiVPN.getInstance().getVpnConfig().mongoDatabaseURL().isEmpty()) { //URL
            ConnectionString cs = new ConnectionString(AntiVPN.getInstance().getVpnConfig().mongoDatabaseURL());
            MongoClientSettings settings = MongoClientSettings.builder().applyConnectionString(cs).build();
            client = MongoClients.create(settings);
        } else {
            MongoClientSettings.Builder settingsBld = MongoClientSettings.builder().readPreference(ReadPreference.nearest())
                    .applyToClusterSettings(builder -> builder.
                            hosts(Collections.singletonList(
                                    new ServerAddress(
                                            AntiVPN.getInstance().getVpnConfig().getIp(),
                                            AntiVPN.getInstance().getVpnConfig().getPort())
                            )));
            if(AntiVPN.getInstance().getVpnConfig().useDatabaseCreds()) {
                settingsBld.credential(MongoCredential
                        .createCredential(AntiVPN.getInstance().getVpnConfig().getUsername(),
                                AntiVPN.getInstance().getVpnConfig().getDatabaseName(),
                                AntiVPN.getInstance().getVpnConfig().getPassword().toCharArray()));
            }

            client = MongoClients.create(settingsBld.build());
        }
        antivpnDatabase = client.getDatabase(AntiVPN.getInstance().getVpnConfig().getDatabaseName());

        settingsDocument = antivpnDatabase.getCollection("settings");

        cacheDocument = antivpnDatabase.getCollection("cache");

        for (Version<MongoVPN> mongoDbVersion : Version.mongoDbVersions) {
            if(mongoDbVersion.needsUpdate(this)) {
                mongoDbVersion.update(this);
            }
        }
    }

    @Override
    public void shutdown() {
        settingsDocument = null;
        cacheDocument = null;
        client.close();
    }
}
