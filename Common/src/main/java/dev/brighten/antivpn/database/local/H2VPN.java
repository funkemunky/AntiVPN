package dev.brighten.antivpn.database.local;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sql.utils.MySQL;
import dev.brighten.antivpn.database.sql.utils.Query;
import dev.brighten.antivpn.utils.MiscUtils;
import dev.brighten.antivpn.web.objects.VPNResponse;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;

public class H2VPN implements VPNDatabase {

    public H2VPN() {
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
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled()|| MySQL.isClosed())
            return Optional.empty();

        ResultSet rs = Query.prepare("SELECT * FROM `responses` WHERE `ip` = ? LIMIT 1").append(ip).executeQuery();

        try {
            if (rs != null && rs.next()) {
                VPNResponse response = new VPNResponse(rs.getString("asn"), rs.getString("ip"),
                        rs.getString("countryName"), rs.getString("countryCode"),
                        rs.getString("city"), rs.getString("timeZone"),
                        rs.getString("method"), rs.getString("isp"), "N/A",
                        rs.getBoolean("proxy"), rs.getBoolean("cached"), true,
                        rs.getDouble("latitude"), rs.getDouble("longitude"),
                        rs.getTimestamp("inserted").getTime(), -1);

                if(System.currentTimeMillis() - response.getLastAccess() > TimeUnit.HOURS.toMillis(1)) {
                    VPNExecutor.threadExecutor.execute(() -> deleteResponse(ip));
                    return Optional.empty();
                }
                return Optional.of(response);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return Optional.empty();
    }

    /*
     * Query.
     * prepare("CREATE TABLE IF NOT EXISTS `responses` (`ip` VARCHAR(45) not null, "
     * +
     * "`countryName` VARCHAR(64), `countryCode` VARCHAR(10), `city` VARCHAR(64), `timeZone` VARCHAR(64), "
     * +
     * "`method` VARCHAR(32), `isp` VARCHAR(32), `proxy` BOOLEAN, `cached` boolean "
     * + "`latitude` DOUBLE, `longitude` DOUBLE)");
     */
    @Override
    public void cacheResponse(VPNResponse toCache) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        Query.prepare("INSERT INTO `responses` (`ip`,`asn`,`countryName`,`countryCode`,`city`,`timeZone`,"
                + "`method`,`isp`,`proxy`,`cached`,`inserted`,`latitude`,`longitude`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")
                .append(toCache.getIp()).append(toCache.getAsn()).append(toCache.getCountryName())
                .append(toCache.getCountryCode()).append(toCache.getCity()).append(toCache.getTimeZone())
                .append(toCache.getMethod()).append(toCache.getIsp()).append(toCache.isProxy())
                .append(toCache.isCached()).append(new Timestamp(System.currentTimeMillis()))
                .append(toCache.getLatitude()).append(toCache.getLongitude()).execute();
    }

    @Override
    public void deleteResponse(String ip) {
        if(!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        Query.prepare("DELETE FROM `responses` WHERE `ip` = ?").append(ip).execute();
    }

    @SneakyThrows
    @Override
    public boolean isWhitelisted(UUID uuid) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return false;
        ResultSet set = Query.prepare("SELECT uuid FROM `whitelisted` WHERE `uuid` = ? LIMIT 1")
                .append(uuid.toString()).executeQuery();

        return set != null && set.next() && set.getString("uuid") != null;
    }

    @SneakyThrows
    @Override
    public boolean isWhitelisted(String ip) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return false;

        if(MiscUtils.isIpv6(ip)) {
            BigDecimal ipv6Decimal = MiscUtils.ipv6ToDecimalFormat(ip);

            try (ResultSet set = Query.prepare("SELECT * FROM `whitelisted-ipv6` WHERE ? BETWEEN `ipstart` AND `ipend`")
                    .append(ipv6Decimal).executeQuery()) {
                return set != null && set.next() && set.getBigDecimal("ipstart") != null;
            }
        } else {
            long ipv4Decimal = MiscUtils.ipv4ToLong(ip);

            try (ResultSet set = Query.prepare("SELECT * FROM `whitelisted-ipv4` WHERE ? BETWEEN `ipstart` AND `ipend`")
                    .append(ipv4Decimal).executeQuery()) {
                return set != null && set.next() && set.getLong("ipstart") != 0;
            }
        }
    }

    @Override
    public void setWhitelisted(UUID uuid, boolean whitelisted) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        if (whitelisted) {
            if (!isWhitelisted(uuid)) {
                Query.prepare("INSERT INTO `whitelisted` (`uuid`) VALUES (?)").append(uuid.toString()).execute();
            }
            AntiVPN.getInstance().getExecutor().getWhitelisted().add(uuid);
        } else {
            Query.prepare("DELETE FROM `whitelisted` WHERE `uuid` = ?").append(uuid.toString()).execute();
            AntiVPN.getInstance().getExecutor().getWhitelisted().remove(uuid);
        }
    }

    @Override
    public void setWhitelisted(String ip, boolean whitelisted) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        boolean isIpv6 = MiscUtils.isIpv6(ip);

        if(whitelisted) {
            if(!isWhitelisted(ip)) {
                if(isIpv6) {
                    BigDecimal ipv6Decimal = MiscUtils.ipv6ToDecimalFormat(ip);
                    Query.prepare("INSERT INTO `whitelisted-ipv6` (`ipstart`, `ipend`) VALUES (?, ?)")
                            .append(ipv6Decimal).append(ipv6Decimal).execute();
                } else {
                    long ipv4Decimal = MiscUtils.ipv4ToLong(ip);

                    Query.prepare("INSERT INTO `whitelisted-ipv4` (`ipstart`, `ipend`) VALUES (?, ?)")
                            .append(ipv4Decimal).append(ipv4Decimal).execute();
                }
            }
            AntiVPN.getInstance().getExecutor().getWhitelistedIps().add(ip);
        } else {
            if(isIpv6) {
                BigDecimal ipv6Decimal = MiscUtils.ipv6ToDecimalFormat(ip);
                Query.prepare("DELETE FROM `whitelisted-ipv6` WHERE ? BETWEEN `ipstart` AND `ipend`")
                        .append(ipv6Decimal).execute();
            } else {
                long ipv4Decimal = MiscUtils.ipv4ToLong(ip);
                Query.prepare("DELETE FROM `whitelisted-ipv4` WHERE ? BETWEEN `ipstart` AND `ipend`")
                        .append(ipv4Decimal).execute();
            }
            AntiVPN.getInstance().getExecutor().getWhitelistedIps().remove(ip);
        }
    }

    @Override
    public List<UUID> getAllWhitelisted() {
        List<UUID> uuids = new ArrayList<>();

        if(!MySQL.isClosed()) Query.prepare("SELECT uuid FROM `whitelisted`")
                .execute(set -> uuids.add(UUID.fromString(set.getString("uuid"))));

        return uuids;
    }

    @Override
    public List<String> getAllWhitelistedIps() {
        if(MySQL.isClosed()) {
            return Collections.emptyList();
        }

        List<String> ips = new ArrayList<>();

        Query.prepare("SELECT * FROM `whitelisted-ipv4`").execute(set -> {
            long start = set.getLong("ipstart");
            long end = set.getLong("ipend");

            for(long i = start; i <= end; i++) {
                ips.add(MiscUtils.decimalToIpv4(i));
            }
        });

        Query.prepare("SELECT * FROM `whitelisted-ipv6`").execute(set -> {
            BigDecimal start = set.getBigDecimal("ipstart");
            BigDecimal end = set.getBigDecimal("ipend");

            for(BigDecimal i = start; i.compareTo(end) <= 0; i = i.add(BigDecimal.ONE)) {
                ips.add(MiscUtils.decimalToIpv6(i));
            }
        });

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
            ResultSet set = Query.prepare("SELECT * FROM `alerts` WHERE `uuid` = ? LIMIT 1")
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
            //We want to make sure there isn't already a uuid inserted to prevent DOUBLE insertions
            alertsState(uuid, alreadyEnabled -> { //No need to make another thread execute, already async
                if(!alreadyEnabled) {
                    Query.prepare("INSERT INTO `alerts` (`uuid`) VALUES (?)").append(uuid.toString())
                            .execute();
                } //No need to insert again of already enabled
            });
            //Removing any uuid FROM the alerts table will disable alerts globally.
        } else VPNExecutor.threadExecutor.execute(() ->
                Query.prepare("DELETE FROM `alerts` WHERE `uuid` = ?")
                        .append(uuid.toString())
                        .execute());
    }

    @Override
    public void clearResponses() {
        if(MySQL.isClosed()) return;

        VPNExecutor.threadExecutor.execute(() -> Query.prepare("DELETE FROM `responses`").execute());
    }

    @Override
    public void init() {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled())
            return;
        log("Initializing H2...");
        MySQL.initH2();

        log("Creating tables...");

        //Running check for old table types to update

        Query.prepare("CREATE TABLE IF NOT EXISTS `whitelisted` (`uuid` VARCHAR(36) NOT NULL)").execute();
        //Query.prepare("CREATE TABLE IF NOT EXISTS `whitelisted-ips` (`ip` VARCHAR(45) NOT NULL)").execute();
        Query.prepare("CREATE TABLE IF NOT EXISTS `whitelisted-ipv4` (`ipstart` BIGINT NOT NULL, `ipend` BIGINT NOT NULL)").execute();
        Query.prepare("CREATE TABLE IF NOT EXISTS `whitelisted-ipv6` (`ipstart` DECIMAL(39, 0) NOT NULL, `ipend` DECIMAL(39, 0) NOT NULL)").execute();
        Query.prepare("CREATE TABLE IF NOT EXISTS `responses` (`ip` VARCHAR(45) NOT NULL, `asn` VARCHAR(12),"
                + "`countryName` TEXT, `countryCode` VARCHAR(10), `city` TEXT, `timeZone` VARCHAR(64), "
                + "`method` VARCHAR(32), `isp` TEXT, `proxy` BOOLEAN, `cached` BOOLEAN, `inserted` TIMESTAMP,"
                + "`latitude` DOUBLE, `longitude` DOUBLE)").execute();
        Query.prepare("CREATE TABLE IF NOT EXISTS `alerts` (`uuid` VARCHAR(36) NOT NULL)").execute();

        log("Creating indexes...");
        try {
            Query.prepare("CREATE INDEX IF NOT EXISTS `uuid_1` ON `whitelisted` (`uuid`)").execute();
            Query.prepare("CREATE INDEX IF NOT EXISTS `ip_1` ON `responses` (`ip`)").execute();
            Query.prepare("CREATE INDEX IF NOT EXISTS `proxy_1` ON `responses` (`proxy`)").execute();
            Query.prepare("CREATE INDEX IF NOT EXISTS `inserted_1` ON `responses` (`inserted`)").execute();
            Query.prepare("CREATE INDEX IF NOT EXISTS `ip_1` ON `whitelisted-ips` (`ip`)").execute();
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().log(Level.SEVERE, "Failed to create indexes for H2 database.");
        }
    }

    @Override
    public void shutdown() {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled())
            return;
        MySQL.shutdown();
    }

    @Override
    public void migrateIpWhitelists() {
        if(MySQL.isClosed())
            return;

        ResultSet rs = Query.prepare("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'whitelisted-ips'").executeQuery();

        try {
            if(rs != null && rs.next()) {
                log("Found old table 'whitelisted-ips', migrating data to modern v1.9.3 format...");
                AtomicInteger recordCount = new AtomicInteger(0);
                Query.prepare("SELECT `ip` ip FROM whitelisted-ips").execute(set -> {
                    String ip = set.getString("ip");

                    recordCount.incrementAndGet();

                    if(MiscUtils.isIpv6(ip)) {
                        BigDecimal ipv6Decimal = MiscUtils.ipv6ToDecimalFormat(ip);

                        Query.prepare("INSERT INTO `whitelisted-ipv6` (`ipstart`, `ipend`) VALUES (?, ?)")
                                .append(ipv6Decimal).append(ipv6Decimal).execute();
                    } else {
                        long ipv4Decimal = MiscUtils.ipv4ToLong(ip);

                        Query.prepare("INSERT INTO `whitelisted-ipv4` (`ipstart`, `ipend`) VALUES (?, ?)")
                                .append(ipv4Decimal).append(ipv4Decimal).execute();
                    }
                });

                log("Checking to see if data migrated successfully before dropping table...");

                // Check the count of records in both whitelisted-ipv4 and ipv6 and see if they equal the count inside of whitelisted-ips.
                try {
                    ResultSet v4Set = Query
                            .prepare("SELECT COUNT(*) v4Count FROM `whitelisted-ipv4`").executeQuery();
                    ResultSet v6Set = Query
                            .prepare("SELECT COUNT(*) v6Count FROM `whitelisted-ipv6`").executeQuery();

                    if(v4Set != null && v4Set.next() && v6Set != null && v6Set.next()) {
                        int v4Count = v4Set.getInt("v4Count");
                        int v6Count = v6Set.getInt("v6Count");

                        if(v4Count + v6Count == recordCount.get()) {
                            log("Data migrated successfully, dropping old table...");
                            if(Query.prepare("DROP TABLE `whitelisted-ips`").execute() == 0) {
                                log("Successfully migrated IP whitelists!");
                            } else {
                                AntiVPN.getInstance().getExecutor().log(Level.SEVERE, "Failed to migrate IP whitelists: Failed to drop old table.");
                            }
                        } else {
                            AntiVPN.getInstance().getExecutor().log(Level.SEVERE, "Failed to migrate IP whitelists: Record count mismatch.");
                        }
                    }
                } catch(SQLException e) {
                    AntiVPN.getInstance().getExecutor().log(Level.SEVERE, "Failed to check if data migrated successfully: " + e.getMessage());
                }
            }
        } catch(SQLException e) {
            AntiVPN.getInstance().getExecutor().log(Level.SEVERE, "Failed to migrate IP whitelists: " + e.getMessage());
        }
    }
    
    private void log(String message) {
        AntiVPN.getInstance().getExecutor().log(message);
    }
}
