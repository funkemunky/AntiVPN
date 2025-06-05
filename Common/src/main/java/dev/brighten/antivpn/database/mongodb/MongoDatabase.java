package dev.brighten.antivpn.database.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.database.Database;
import dev.brighten.antivpn.database.mongodb.records.AlertsUser;
import dev.brighten.antivpn.database.mongodb.records.CidrWhitelist;
import dev.brighten.antivpn.database.mongodb.records.UserIpResponse;
import dev.brighten.antivpn.database.mongodb.records.UserWhitelist;
import dev.brighten.antivpn.utils.CIDRUtils;
import dev.brighten.antivpn.utils.IpUtils;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.web.objects.VPNResponse;
import org.bson.Document;

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MongoDatabase implements Database {

    private MongoCollection<AlertsUser> alertsCollection;
    private MongoCollection<CidrWhitelist> cidrWhitelistCollection;
    private MongoCollection<UserWhitelist> userWhitelistCollection;
    private MongoCollection<UserIpResponse> vpnResponseCollection;

    @Override
    public Optional<VPNResponse> getStoredResponse(String ip) {
       UserIpResponse response = vpnResponseCollection.find(Filters.eq("ip", ip)).first();

       if(response == null) {
           return Optional.empty();
       }

        try {
            return Optional.of(response.getVpnResponse());
        } catch (JSONException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not convert vpn response from JSON String to DTO " +
                    "for address: " + ip, e);
            return Optional.empty();
        }
    }

    @Override
    public void cacheResponse(VPNResponse toCache) {
        try {
            UserIpResponse response = new UserIpResponse(toCache.getIp(), new Date(), toCache.toJson().toString());

            vpnResponseCollection.updateOne(Filters.eq("ip", toCache.getIp()),
                    new Document("$set", response),
                    new UpdateOptions().upsert(true));
        } catch (JSONException e) {
            AntiVPN.getInstance().getExecutor().log(Level.SEVERE, "An error occurred while caching response", e);
        }
    }

    @Override
    public void deleteResponse(String ip) {
        vpnResponseCollection.deleteOne(Filters.eq("ip", ip));
    }

    @Override
    public void clearResponses() {
        //Clears all documents within the collection
        var result = vpnResponseCollection.deleteMany(new Document());

        AntiVPN.getInstance().getExecutor().log(Level.INFO, "VPN responses have been cleared (count=%s)", result.getDeletedCount());
    }

    @Override
    public void clearOutdatedResponses() {
        var result = vpnResponseCollection.deleteMany(Filters.lte("date",
                new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMicros(7))));

        AntiVPN.getInstance().getExecutor().log(Level.INFO, "Cleared outdated responses (count=%s)",
                result.getDeletedCount());
    }

    @Override
    public boolean isWhitelisted(UUID uuid) {
        UserWhitelist whitelist = userWhitelistCollection.find(Filters.eq("uuid", uuid)).first();

        return whitelist != null;
    }

    @Override
    public boolean isWhitelisted(String ip) {
        Optional<BigDecimal> ipDec = IpUtils.getIpDecimal(ip);

        if(ipDec.isEmpty()) {
            AntiVPN.getInstance().getExecutor().log(Level.WARNING, "Could not check for whitelist on IP %s since" +
                    " it cannot be converted to decimal!", ip);
            return false;
        }

        BigDecimal decimal = ipDec.get();

        CidrWhitelist whitelist = cidrWhitelistCollection.find(Filters.and(
                Filters.lte("start", decimal),
                Filters.gte("end", decimal)

        )).first();

        return whitelist != null;
    }

    @Override
    public void addWhitelist(UUID uuid) {
        UserWhitelist whitelist = new UserWhitelist(uuid);

        userWhitelistCollection.insertOne(whitelist);
    }

    @Override
    public void addWhitelist(String cidr) {
        try {
            var cidrObj = new CIDRUtils(cidr);

            Optional<BigDecimal> start = IpUtils.getIpDecimal(cidrObj.getStartAddress().getHostAddress()),
                    end = IpUtils.getIpDecimal(cidrObj.getEndAddress().getHostAddress());

            if(start.isEmpty() || end.isEmpty()) {
                AntiVPN.getInstance().getExecutor().log(Level.WARNING, "Could not whitelist cidr %s since " +
                        "it is missing either a start or end address", cidr);
                return;
            }

            CidrWhitelist whitelist = new CidrWhitelist(start.get(), end.get());

            cidrWhitelistCollection.insertOne(whitelist);
        } catch (UnknownHostException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not whitelist cidr: " + cidr, e);
        }
    }

    @Override
    public void removeWhitelist(UUID uuid) {
        var result = userWhitelistCollection.deleteMany(Filters.eq("uuid", uuid.toString()));

        AntiVPN.getInstance().getExecutor().log(Level.INFO, "Removed whitelist for uuid %s (count=%s)",
                uuid, result.getDeletedCount());
    }

    @Override
    public void removeWhitelist(String cidr) {
        try {
            var cidrObj = new CIDRUtils(cidr);

            Optional<BigDecimal> start = IpUtils.getIpDecimal(cidrObj.getStartAddress().getHostAddress());
            Optional<BigDecimal> end = IpUtils.getIpDecimal(cidrObj.getEndAddress().getHostAddress());

            if(start.isEmpty() || end.isEmpty()) {
                AntiVPN.getInstance().getExecutor().log(Level.WARNING, "Could not remove cidr %s from whitelist" +
                        " since it is missing either a start or end address.", cidr);
                return;
            }

            var result = cidrWhitelistCollection.deleteMany(Filters.and(Filters.eq("start", start.get()),
                    Filters.eq("end", end.get())));

            AntiVPN.getInstance().getExecutor().log(Level.INFO, "Removed cidr %s from whitelist (count=%s).",
                    cidr, result.getDeletedCount());
        } catch (UnknownHostException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not remove whitelist for CIDR: " + cidr, e);
        }
    }

    @Override
    public boolean getAlertsState(UUID uuid) {
        AlertsUser alertsUser = alertsCollection.find(Filters.eq("uuid", uuid)).first();

        if(alertsUser == null) {
            return false;
        }

        return alertsUser.state();
    }

    @Override
    public void updateAlertsState(UUID uuid, boolean state) {
        alertsCollection.updateOne(Filters.eq("uuid", uuid),
                new Document("$set", new AlertsUser(uuid, state)),
                new UpdateOptions().upsert(true));
    }

    @Override
    public void init() {
        String connectionUrl;
        if(AntiVPN.getInstance().getVpnConfig().getMongoURL().isEmpty()) {
            String databaseName = AntiVPN.getInstance().getVpnConfig().getDatabaseName();
            String username = AntiVPN.getInstance().getVpnConfig().getUsername();
            String password = AntiVPN.getInstance().getVpnConfig().getPassword();
            String ip = AntiVPN.getInstance().getVpnConfig().getIp();
            int port = AntiVPN.getInstance().getVpnConfig().getPort();

            connectionUrl = String.format("mongodb+srv://" +
                            "%s:%s>@%s:%s/%s?connectTimeoutMS=2000",
                    username, password, ip, port, databaseName);
        } else {
            connectionUrl = AntiVPN.getInstance()
                    .getVpnConfig().getMongoURL();
        }

        try(MongoClient mongoClient = MongoClients.create(connectionUrl)) {
            var database = mongoClient.getDatabase(AntiVPN.getInstance().getVpnConfig().getDatabaseName());

            userWhitelistCollection = database.getCollection("whitelist", UserWhitelist.class);
            cidrWhitelistCollection = database.getCollection("cidrWhitelist", CidrWhitelist.class);
            alertsCollection = database.getCollection("alerts", AlertsUser.class);
            vpnResponseCollection = database.getCollection("responses", UserIpResponse.class);
        }
    }

    @Override
    public void shutdown() {
        userWhitelistCollection = null;
        cidrWhitelistCollection = null;
        alertsCollection = null;
        vpnResponseCollection = null;
    }
}
