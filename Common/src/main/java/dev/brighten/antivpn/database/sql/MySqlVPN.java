package dev.brighten.antivpn.database.sql;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sql.utils.MySQL;
import dev.brighten.antivpn.database.sql.utils.Query;
import dev.brighten.antivpn.utils.VPNResponse;
import lombok.SneakyThrows;

import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

// For logging using getLogger if Bukkit;
import org.bukkit.Bukkit;

public class MySqlVPN implements VPNDatabase {

    private Thread whitelistedThread;

    public MySqlVPN() {
        whitelistedThread = new Thread(() -> {
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(8));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true) {
                // Updating from database
                if (AntiVPN.getInstance().getConfig().isDatabaseEnabled()) {
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

        whitelistedThread.start();
    }

    @Override
    public Optional<VPNResponse> getStoredResponse(String ip) {
        if (!AntiVPN.getInstance().getConfig().isDatabaseEnabled()|| MySQL.isClosed())
            return Optional.empty();

        ResultSet rs = Query.prepare("select * from `responses` where `ip` = ? limit 1").append(ip).executeQuery();

        try {
            if (rs != null && rs.next()) {
                VPNResponse response = new VPNResponse(rs.getString("asn"), rs.getString("ip"),
                        rs.getString("countryName"), rs.getString("countryCode"),
                        rs.getString("city"), rs.getString("timeZone"),
                        rs.getString("method"), rs.getString("isp"), "N/A",
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
        if (!AntiVPN.getInstance().getConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        Query.prepare("insert into `responses` (`ip`,`asn`,`countryName`,`countryCode`,`city`,`timeZone`,"
                + "`method`,`isp`,`proxy`,`cached`,`inserted`,`latitude`,`longitude`) values (?,?,?,?,?,?,?,?,?,?,?,?,?)")
                .append(toCache.getIp()).append(toCache.getAsn()).append(toCache.getCountryName())
                .append(toCache.getCountryCode()).append(toCache.getCity()).append(toCache.getTimeZone())
                .append(toCache.getMethod()).append(toCache.getIsp()).append(toCache.isProxy())
                .append(toCache.isCached()).append(new Timestamp(System.currentTimeMillis()))
                .append(toCache.getLatitude()).append(toCache.getLongitude()).execute();
    }

    @SneakyThrows
    @Override
    public boolean isWhitelisted(UUID uuid) {
        if (!AntiVPN.getInstance().getConfig().isDatabaseEnabled() || MySQL.isClosed())
            return false;
        ResultSet set = Query.prepare("select uuid from `whitelisted` where `uuid` = ? limit 1").append(uuid.toString())
                .executeQuery();

        return set != null && set.next() && set.getString("uuid") != null;
    }

    @Override
    public void setWhitelisted(UUID uuid, boolean whitelisted) {
        if(MySQL.isClosed()) return;

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
    public List<UUID> getAllWhitelisted() {
        List<UUID> uuids = new ArrayList<>();

        if(MySQL.isClosed()) return uuids;

        ResultSet set = Query.prepare("select uuid from `whitelisted`").executeQuery();

        try {
            while (set.next()) {
                uuids.add(UUID.fromString(set.getString("uuid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return uuids;
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
    public void alertsState(UUID uuid, Consumer<Boolean> result) {
        if(MySQL.isClosed()) return;

        VPNExecutor.threadExecutor.execute(() -> {
            ResultSet set = Query.prepare("select * from `alerts` where `uuid` = ? limit 1")
                    .append(uuid.toString()).executeQuery();

            try {
                result.accept(set != null && set.next() && set.getString("uuid") != null);
            } catch (SQLException e) {
                e.printStackTrace();
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
    public void init() {
        if (!AntiVPN.getInstance().getConfig().isDatabaseEnabled())
            return;
        if (AntiVPN.getInstance().getConfig().isBukkit())
            Bukkit.getLogger().info("Initializing MySQL...");
        else
            System.out.println("Initializing MySQL...");
        MySQL.init();

        if (AntiVPN.getInstance().getConfig().isBukkit())
            Bukkit.getLogger().info("Creating tables...");
        else
            System.out.println("Creating tables...");

        //Running check for old table types to update
        oldTableCheck: {
            Query.prepare("select `DATA_TYPE` from INFORMATION_SCHEMA.COLUMNS " +
                    "WHERE table_name = 'responses' AND COLUMN_NAME = 'isp';").execute(set -> {
                        if(set.getObject("DATA_TYPE").toString().contains("varchar")) {
                            System.out.println("Using old database format for storing responses! " +
                                    "Dropping table and creating a new one...");
                            if(Query.prepare("drop table `responses`").execute() > 0) {
                                System.out.println("Successfully dropped table!");
                            }
                        }
            });
        }

        Query.prepare("create table if not exists `whitelisted` (`uuid` varchar(36) not null)").execute();
        Query.prepare("create table if not exists `responses` (`ip` varchar(45) not null, `asn` varchar(12),"
                + "`countryName` text, `countryCode` varchar(10), `city` text, `timeZone` varchar(64), "
                + "`method` varchar(32), `isp` text, `proxy` boolean, `cached` boolean, `inserted` timestamp,"
                + "`latitude` double, `longitude` double)").execute();
        Query.prepare("create table if not exists `alerts` (`uuid` varchar(36) not null)").execute();

        System.out.println("Creating indexes...");
        try {
            // Ref:
            // https://dba.stackexchange.com/questions/24531/mysql-create-index-if-not-exists
            String query = "SELECT COUNT(1) IndexExists FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='whitelisted' AND index_name='uuid_1';";
            ResultSet rs = Query.prepare(query).executeQuery();
            int id = 0;
            while (rs.next()) {
                id = rs.getInt("IndexExists");
            }
            if (id == 0) {
                Query.prepare("create index `uuid_1` on `whitelisted` (`uuid`)").execute();
            }
            id = 0;
            query = "SELECT COUNT(1) IndexExists FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='responses' AND index_name='ip_1';";
            rs = Query.prepare(query).executeQuery();
            while (rs.next()) {
                id = rs.getInt("IndexExists");
            }
            if (id == 0) {
                Query.prepare("create index `ip_1` on `responses` (`ip`)").execute();
            }
            id = 0;
            query = "SELECT COUNT(1) IndexExists FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='responses' AND index_name='proxy_1';";
            rs = Query.prepare(query).executeQuery();
            while (rs.next()) {
                id = rs.getInt("IndexExists");
            }
            if (id == 0) {
                Query.prepare("create index `proxy_1` on `responses` (`proxy`)").execute();
            }
            id = 0;
            query = "SELECT COUNT(1) IndexExists FROM INFORMATION_SCHEMA.STATISTICS WHERE table_schema=DATABASE() AND table_name='responses' AND index_name='inserted_1';";
            rs = Query.prepare(query).executeQuery();
            while (rs.next()) {
                id = rs.getInt("IndexExists");
            }
            if (id == 0) {
                Query.prepare("create index `inserted_1` on `responses` (`inserted`)").execute();
            }
        } catch (Exception e) {
            System.err.println("MySQL Excepton created" + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (!AntiVPN.getInstance().getConfig().isDatabaseEnabled())
            return;
        MySQL.shutdown();
    }
}
