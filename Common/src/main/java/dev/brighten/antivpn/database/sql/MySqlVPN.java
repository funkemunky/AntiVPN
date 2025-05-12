package dev.brighten.antivpn.database.sql;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sql.utils.MySQL;
import dev.brighten.antivpn.database.sql.utils.Query;
import dev.brighten.antivpn.web.objects.VPNResponse;
import lombok.SneakyThrows;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MySqlVPN implements VPNDatabase {

    private final Cache<String, VPNResponse> cachedResponses = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .maximumSize(4000)
            .build();


    public MySqlVPN() {
        VPNExecutor.threadExecutor.scheduleAtFixedRate(() -> {
            if(!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed()) return;

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
        if (isDisabled())
            return Optional.empty();

        VPNResponse response = cachedResponses.get(ip, ip2 -> {
            try(ResultSet rs = Query.prepare("select * from `responses` where `ip` = ? limit 1").append(ip)
                    .executeQuery()) {
                if (rs != null && rs.next()) {
                    VPNResponse responseFromDoc = new VPNResponse(rs.getString("asn"),
                            rs.getString("ip"),
                            rs.getString("countryName"), rs.getString("countryCode"),
                            rs.getString("city"), rs.getString("timeZone"),
                            rs.getString("method"), rs.getString("isp"), "N/A",
                            rs.getBoolean("proxy"), rs.getBoolean("cached"), true,
                            rs.getDouble("latitude"), rs.getDouble("longitude"),
                            rs.getTimestamp("inserted").getTime(), -1);

                    if(System.currentTimeMillis() - responseFromDoc.getLastAccess() > TimeUnit.HOURS.toMillis(1)) {
                        VPNExecutor.threadExecutor.execute(() -> deleteResponse(ip));
                        return null;
                    }

                    return responseFromDoc;
                }
            } catch (SQLException e) {
                AntiVPN.getInstance().getExecutor()
                        .logException("Failed to get response from cache due to SQL error for: " + ip, e);
            }
            return null;
        });

        return Optional.ofNullable(response);
    }

    /*
     * Query.
     * prepare("create table if not exists `responses` (`ip` varchar(45) not null, "
     * +
     * "`countryName` varchar(64), `countryCode` varchar(10), `city` varchar(64), `timeZone` varchar(64), "
     * +
     * "`method` varchar(32), `isp` varchar(32), `proxy` boolean, `cached` boolean "
     * + "`latitude` double, `longitude` double)");
     */
    @Override
    public void cacheResponse(VPNResponse toCache) {
        if (isDisabled())
            return;

        cachedResponses.put(toCache.getIp(), toCache);

        Query.prepare("insert into `responses` (`ip`,`asn`,`countryName`,`countryCode`,`city`,`timeZone`,"
                + "`method`,`isp`,`proxy`,`cached`,`inserted`,`latitude`,`longitude`) values (?,?,?,?,?,?,?,?,?,?,?,?,?)")
                .append(toCache.getIp()).append(toCache.getAsn()).append(toCache.getCountryName())
                .append(toCache.getCountryCode()).append(toCache.getCity()).append(toCache.getTimeZone())
                .append(toCache.getMethod()).append(toCache.getIsp()).append(toCache.isProxy())
                .append(toCache.isCached()).append(new Timestamp(System.currentTimeMillis()))
                .append(toCache.getLatitude()).append(toCache.getLongitude()).execute();
    }

    @Override
    public void deleteResponse(String ip) {
        if(!isDisabled())
            return;

        Query.prepare("delete from `responses` where `ip` = ?").append(ip).execute();
    }

    @SneakyThrows
    @Override
    public boolean isWhitelisted(UUID uuid) {
        if (isDisabled())
            return false;
        try(ResultSet set = Query.prepare("select uuid from `whitelisted` where `uuid` = ? limit 1")
                .append(uuid.toString()).executeQuery()) {
            return set != null && set.next() && set.getString("uuid") != null;
        }
    }

    @SneakyThrows
    @Override
    public boolean isWhitelisted(String ip) {
        if (isDisabled())
            return false;
        try(ResultSet set = Query.prepare("select `ip` from `whitelisted-ips` where `ip` = ? limit 1")
                .append(ip).executeQuery()) {
            return set != null && set.next() && set.getString("ip") != null;
        }
    }

    @Override
    public void setWhitelisted(UUID uuid, boolean whitelisted) {
        if (isDisabled())
            return;

        if (whitelisted) {
            if (!isWhitelisted(uuid)) {
                Query.prepare("insert into `whitelisted` (`uuid`) values (?)").append(uuid.toString()).execute();
            }
            AntiVPN.getInstance().getExecutor().getWhitelisted().add(uuid);
        } else {
            Query.prepare("delete from `whitelisted` where `uuid` = ?").append(uuid.toString()).execute();
            AntiVPN.getInstance().getExecutor().getWhitelisted().remove(uuid);
        }
    }

    @Override
    public void setWhitelisted(String ip, boolean whitelisted) {
        if (isDisabled())
            return;

        if(whitelisted) {
            if(!isWhitelisted(ip)) {
                Query.prepare("insert into `whitelisted-ips` (`ip`) values (?)").append(ip).execute();
            }
            AntiVPN.getInstance().getExecutor().getWhitelistedIps().add(ip);
        } else {
            Query.prepare("delete from `whitelisted-ips` where `ip` = ?").append(ip).execute();
            AntiVPN.getInstance().getExecutor().getWhitelistedIps().remove(ip);
        }
    }

    @Override
    public List<UUID> getAllWhitelisted() {
        List<UUID> uuids = new ArrayList<>();

        if(!MySQL.isClosed()) Query.prepare("select uuid from `whitelisted`")
                .execute(set -> uuids.add(UUID.fromString(set.getString("uuid"))));

        return uuids;
    }

    @Override
    public List<String> getAllWhitelistedIps() {
        List<String> ips = new ArrayList<>();

        if(!MySQL.isClosed()) Query.prepare("select `ip` from `whitelisted-ips`")
                    .execute(set -> ips.add(set.getString("ip")));

        return ips;
    }

    @Override
    public void getStoredResponseAsync(String ip, Consumer<Optional<VPNResponse>> result) {
        if(MySQL.isClosed()) return;

        VPNExecutor.threadExecutor.execute(() -> result.accept(getStoredResponse(ip)));
    }

    @Override
    public void isWhitelistedAsync(UUID uuid, Consumer<Boolean> result) {
        if(MySQL.isClosed()) return;

        VPNExecutor.threadExecutor.execute(() -> result.accept(isWhitelisted(uuid)));
    }

    @Override
    public void isWhitelistedAsync(String ip, Consumer<Boolean> result) {
        if(MySQL.isClosed()) return;

        VPNExecutor.threadExecutor.execute(() -> result.accept(isWhitelisted(ip)));
    }

    @Override
    public void alertsState(UUID uuid, Consumer<Boolean> result) {
        if(MySQL.isClosed()) return;

        VPNExecutor.threadExecutor.execute(() -> {

            try(ResultSet set = Query.prepare("select * from `alerts` where `uuid` = ? limit 1")
                    .append(uuid.toString()).executeQuery()) {
                result.accept(set != null && set.next() && set.getString("uuid") != null);
            } catch (SQLException e) {
                AntiVPN.getInstance().getExecutor()
                        .logException("Failed to get alerts state from database for: " + uuid, e);
                result.accept(false);
            }
        });
    }

    @Override
    public void updateAlertsState(UUID uuid, boolean enabled) {
        if(MySQL.isClosed()) return;

        if(enabled) {
            //We want to make sure there isn't already a uuid inserted to prevent double insertions
            alertsState(uuid, alreadyEnabled -> { //No need to make another thread execute, already async
                if(!alreadyEnabled) {
                    Query.prepare("insert into `alerts` (`uuid`) values (?)").append(uuid.toString())
                            .execute();
                } //No need to insert again of already enabled
            });
            //Removing any uuid from the alerts table will disable alerts globally.
        } else VPNExecutor.threadExecutor.execute(() ->
                Query.prepare("delete from `alerts` where `uuid` = ?")
                        .append(uuid.toString())
                        .execute());
    }

    @Override
    public void clearResponses() {
        if(MySQL.isClosed()) return;

        VPNExecutor.threadExecutor.execute(() -> Query.prepare("delete from `responses`").execute());
    }

    @Override
    public void init() {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled())
            return;
        AntiVPN.getInstance().getExecutor().log("Initializing MySQL...");
        MySQL.init();

        AntiVPN.getInstance().getExecutor().log("Creating tables...");

        //Running check for old table types to update
        Query.prepare("select `DATA_TYPE` from INFORMATION_SCHEMA.COLUMNS " +
                "WHERE table_name = 'responses' AND COLUMN_NAME = 'isp';").execute(set -> {
            if(set.getObject("DATA_TYPE").toString().contains("varchar")) {
                AntiVPN.getInstance().getExecutor().log("Using old database format for storing responses! " +
                        "Dropping table and creating a new one...");
                if(Query.prepare("drop table `responses`").execute() > 0) {
                    AntiVPN.getInstance().getExecutor().log("Successfully dropped table!");
                }
            }
        });

        Query.prepare("create table if not exists `whitelisted` (`uuid` varchar(36) not null)").execute();
        Query.prepare("create table if not exists `whitelisted-ips` (`ip` varchar(45) not null)").execute();
        Query.prepare("create table if not exists `responses` (`ip` varchar(45) not null, `asn` varchar(12),"
                + "`countryName` text, `countryCode` varchar(10), `city` text, `timeZone` varchar(64), "
                + "`method` varchar(32), `isp` text, `proxy` boolean, `cached` boolean, `inserted` timestamp,"
                + "`latitude` double, `longitude` double)").execute();
        Query.prepare("create table if not exists `alerts` (`uuid` varchar(36) not null)").execute();

        AntiVPN.getInstance().getExecutor().log("Creating indexes...");
        try {
            // Ref:
            // https://dba.stackexchange.com/questions/24531/mysql-create-index-if-not-exists

            String query = "SELECT COUNT(1) IndexExists FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE()" +
                    " AND table_name='whitelisted' AND index_name='uuid_1';";
            ResultSet rs = Query.prepare(query).executeQuery();
            int id = 0;
            while (rs.next()) {
                id = rs.getInt("IndexExists");
            }
            if (id == 0) {
                Query.prepare("create index `uuid_1` on `whitelisted` (`uuid`)").execute();
            }
            id = 0;
            responsesIndex: {
                query = "SELECT COUNT(1) IndexExists FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() " +
                        "AND table_name='responses' AND index_name='ip_1';";
                rs = Query.prepare(query).executeQuery();
                while (rs.next()) {
                    id = rs.getInt("IndexExists");
                }
                if (id == 0) {
                    Query.prepare("create index `ip_1` on `responses` (`ip`)").execute();
                }
                id = 0;
                query = "SELECT COUNT(1) IndexExists FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() " +
                        "AND table_name='responses' AND index_name='proxy_1';";
                rs = Query.prepare(query).executeQuery();
                while (rs.next()) {
                    id = rs.getInt("IndexExists");
                }
                if (id == 0) {
                    Query.prepare("create index `proxy_1` on `responses` (`proxy`)").execute();
                }
                id = 0;
                query = "SELECT COUNT(1) IndexExists FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE()" +
                        " AND table_name='responses' AND index_name='inserted_1';";
                rs = Query.prepare(query).executeQuery();
                while (rs.next()) {
                    id = rs.getInt("IndexExists");
                }
                if (id == 0) {
                    Query.prepare("create index `inserted_1` on `responses` (`inserted`)").execute();
                }
                id = 0;
            }
            whitelistedIpsIndex: {
                query = "SELECT COUNT(1) IndexExists FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE()" +
                        " AND table_name='whitelisted-ips' AND index_name='ip_1';";
                rs = Query.prepare(query).executeQuery();
                while (rs.next()) {
                    id = rs.getInt("IndexExists");
                }
                if (id == 0) {
                    Query.prepare("create index `ip_1` on `whitelisted-ips` (`ip`)").execute();
                }
            }
        } catch (Exception e) {
            System.err.println("MySQL Excepton created" + e.getMessage());
        }
    }
    
    private boolean isDisabled() {
        return !AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled()|| MySQL.isClosed();
    }

    @Override
    public void shutdown() {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled())
            return;
        MySQL.shutdown();
    }
}
