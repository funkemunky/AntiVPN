package dev.brighten.antivpn.database.sql;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sql.utils.MySQL;
import dev.brighten.antivpn.database.sql.utils.Query;
import dev.brighten.antivpn.utils.VPNResponse;
import lombok.SneakyThrows;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MySqlVPN implements VPNDatabase {

    private Thread whitelistedThread;

    public MySqlVPN() {
        whitelistedThread = new Thread(() -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(8));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while(true) {
                //Updating from database
                if(AntiVPN.getInstance().getConfig().isDatabaseEnabled()) {
                    AntiVPN.getInstance().getExecutor().getWhitelisted().clear();
                    AntiVPN.getInstance().getExecutor().getWhitelisted()
                            .addAll(AntiVPN.getInstance().getDatabase().getAllWhitelisted());
                }
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(4));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        whitelistedThread.start();;
    }
    @Override
    public Optional<VPNResponse> getStoredResponse(String ip) {
        if(!AntiVPN.getInstance().getConfig().isDatabaseEnabled()) return Optional.empty();
        ResultSet rs = Query.prepare("select * from `responses` where `ip` = ? limit 1")
                .append(ip).executeQuery();

        try {
            if(rs != null && !rs.wasNull() && rs.next()) {
                VPNResponse response = new VPNResponse(rs.getString("asn"), rs.getString("ip"),
                        rs.getString("countryName"), rs.getString("countryCode"),
                        rs.getString("city"), rs.getString("timeZone"),
                        rs.getString("method"), rs.getString("isp"),
                        rs.getBoolean("proxy"), rs.getBoolean("cached"), true,
                        rs.getDouble("latitude"), rs.getDouble("longitude"),
                        System.currentTimeMillis(), -1);
                return Optional.of(response);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return Optional.empty();
    }

    /*Query.prepare("create table if not exists `responses` (`ip` varchar(45) not null, " +
                "`countryName` varchar(64), `countryCode` varchar(10), `city` varchar(64), `timeZone` varchar(64), " +
                "`method` varchar(32), `isp` varchar(32), `proxy` boolean, `cached` boolean " +
                "`latitude` double, `longitude` double)");*/
    @Override
    public void cacheResponse(VPNResponse toCache) {
        if(!AntiVPN.getInstance().getConfig().isDatabaseEnabled()) return;
        Query.prepare("insert into `responses` (`ip`,`asn`,`countryName`,`countryCode`,`city`,`timeZone`," +
                "`method`,`isp`,`proxy`,`cached`,`inserted`,`latitude`,`longitude`) values (?,?,?,?,?,?,?,?,?,?,?,?,?)")
                .append(toCache.getIp()).append(toCache.getAsn()).append(toCache.getCountryName())
                .append(toCache.getCountryCode()).append(toCache.getCity())
                .append(toCache.getTimeZone()).append(toCache.getMethod()).append(toCache.getIsp())
                .append(toCache.isProxy()).append(toCache.isCached()).append(new Timestamp(System.currentTimeMillis()))
                .append(toCache.getLatitude()).append(toCache.getLongitude()).execute();
    }

    @SneakyThrows
    @Override
    public boolean isWhitelisted(UUID uuid) {
        if(!AntiVPN.getInstance().getConfig().isDatabaseEnabled()) return false;
        ResultSet set = Query.prepare("select uuid from `whitelisted` where `uuid` = ? limit 1")
                .append(uuid.toString()).executeQuery();


        return set != null && !set.wasNull() && set.next() && set.getString("uuid") != null;
    }

    @Override
    public void setWhitelisted(UUID uuid, boolean whitelisted) {
        if(whitelisted) {
            if(!isWhitelisted(uuid)) {
                Query.prepare("insert into `whitelisted` (`uuid`) values (?)").append(uuid.toString()).execute();
            }
            AntiVPN.getInstance().getExecutor().getWhitelisted().add(uuid);
        } else {
            Query.prepare("delete from `whitelisted` where `uuid` = ?").append(uuid.toString()).execute();
            AntiVPN.getInstance().getExecutor().getWhitelisted().remove(uuid);
        }
    }

    @Override
    public List<UUID> getAllWhitelisted() {
        List<UUID> uuids = new ArrayList<>();
        ResultSet set = Query.prepare("select uuid from `whitelisted`").executeQuery();

        try {
            while(set.next()) {
                uuids.add(UUID.fromString(set.getString("uuid")));
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return uuids;
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
    public void init() {
        if(!AntiVPN.getInstance().getConfig().isDatabaseEnabled()) return;
        System.out.println("Initializing MySQL...");
        MySQL.init();

        System.out.println("Creating tables...");
        Query.prepare("create table if not exists `whitelisted` (`uuid` varchar(36) not null)").execute();
        Query.prepare("create table if not exists `responses` (`ip` varchar(45) not null, `asn` varchar(12)," +
                "`countryName` varchar(64), `countryCode` varchar(10), `city` varchar(64), `timeZone` varchar(64), " +
                "`method` varchar(32), `isp` varchar(64), `proxy` boolean, `cached` boolean, `inserted` timestamp," +
                "`latitude` double, `longitude` double)").execute();

        System.out.println("Creating indexes...");
        Query.prepare("create index if not exists `uuid_1` on `whitelisted` (`uuid`)").execute();
        Query.prepare("create index if not exists `ip_1` on `responses` (`ip`)").execute();
        Query.prepare("create index if not exists `proxy_1` on `responses` (`proxy`)").execute();
        Query.prepare("create index if not exists `inserted_1` on `responses` (`inserted`)").execute();
    }

    @Override
    public void shutdown() {
        if(!AntiVPN.getInstance().getConfig().isDatabaseEnabled()) return;
        MySQL.shutdown();
    }
}
