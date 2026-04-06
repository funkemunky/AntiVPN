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

package dev.brighten.antivpn.database.local;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sql.utils.ExecutableStatement;
import dev.brighten.antivpn.database.sql.utils.MySQL;
import dev.brighten.antivpn.database.sql.utils.Query;
import dev.brighten.antivpn.database.version.Version;
import dev.brighten.antivpn.utils.CIDRUtils;
import dev.brighten.antivpn.web.objects.VPNResponse;
import lombok.SneakyThrows;

import java.io.File;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class H2VPN implements VPNDatabase {

    private final Cache<String, VPNResponse> cachedResponses = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .maximumSize(4000)
            .build();


    public H2VPN() {
        AntiVPN.getInstance().getExecutor().getThreadExecutor().scheduleAtFixedRate(() -> {
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

        VPNResponse response = cachedResponses.get(ip, ip2 -> {
            try(ExecutableStatement statement = Query.prepare("select * from `responses` where `ip` = ? limit 1").append(ip)) {
                try(ResultSet rs = statement.executeQuery()) {
                    if (rs != null && rs.next()) {
                        return new VPNResponse(rs.getString("asn"), rs.getString("ip"),
                                rs.getString("countryName"), rs.getString("countryCode"),
                                rs.getString("city"), rs.getString("timeZone"),
                                rs.getString("method"), rs.getString("isp"), "N/A",
                                rs.getBoolean("proxy"), rs.getBoolean("cached"), true,
                                rs.getDouble("latitude"), rs.getDouble("longitude"),
                                rs.getTimestamp("inserted").getTime(), -1);
                    }
                }
            } catch (SQLException e) {
                AntiVPN.getInstance().getExecutor().logException("There was a problem getting a response for "
                        + ip, e);
            } catch (Exception e) {
                throw new RuntimeException(e);
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
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        if(AntiVPN.getInstance().getVpnConfig().cachedResults()) {
            cachedResponses.put(toCache.getIp(), toCache);

            try(var statement = Query.prepare("insert into `responses` (`ip`,`asn`,`countryName`,`countryCode`,`city`,`timeZone`,"
                            + "`method`,`isp`,`proxy`,`cached`,`inserted`,`latitude`,`longitude`) values (?,?,?,?,?,?,?,?,?,?,?,?,?)")
                    .append(toCache.getIp()).append(toCache.getAsn()).append(toCache.getCountryName())
                    .append(toCache.getCountryCode()).append(toCache.getCity()).append(toCache.getTimeZone())
                    .append(toCache.getMethod()).append(toCache.getIsp()).append(toCache.isProxy())
                    .append(toCache.isCached()).append(new Timestamp(System.currentTimeMillis()))
                    .append(toCache.getLatitude()).append(toCache.getLongitude())) {
                statement.execute();
            } catch(SQLException e) {
                AntiVPN.getInstance().getExecutor().logException("Could not cache response for IP: " + toCache.getIp(), e);
            }
        }
    }

    @Override
    public void deleteResponse(String ip) {
        if(!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        try(var statement = Query.prepare("delete from `responses` where `ip` = ?").append(ip)) {
            statement.execute();
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not delete response from IP: " + ip, e);
        }
    }

    @Override
    public boolean isWhitelisted(UUID uuid) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return false;
        try(var statement = Query.prepare("select uuid from `whitelisted` where `uuid` = ? limit 1")
                .append(uuid.toString())) {
            try(var set = statement.executeQuery()) {
                return set != null && set.next() && set.getString("uuid") != null;
            }
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not check whitelist for uuid '" + uuid + "' due to SQL error.", e);
            return false;
        }
    }

    @SneakyThrows
    @Override
    public boolean isWhitelisted(String cidr) {
        return isWhitelisted(new CIDRUtils(cidr));
    }

    @Override
    public boolean isWhitelisted(CIDRUtils cidr) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return false;

        BigInteger start = cidr.getStartIpInt();
        BigInteger end = cidr.getEndIpInt();

        try(var statement = Query.prepare("SELECT * FROM `whitelisted-ranges` WHERE ip_start <= ? AND ip_end >= ?")
                .append(start).append(end)) {

            try(var result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not check whitelist for cidr '" + cidr + "' due to SQL error.", e);
        }
        return false;
    }

    @Override
    public void addWhitelist(UUID uuid) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        try(var statement = Query.prepare("insert into `whitelisted` (`uuid`) values (?)").append(uuid.toString())) {
            statement.execute();
            AntiVPN.getInstance().getExecutor().getWhitelisted().add(uuid);
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not add uuid '" + uuid + "' to whitelist due to SQL error.", e);
        }
    }

    @Override
    public void removeWhitelist(UUID uuid) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;
        try(var statement = Query.prepare("delete from `whitelisted` where `uuid` = ?").append(uuid.toString())) {
            statement.execute();
            AntiVPN.getInstance().getExecutor().getWhitelisted().remove(uuid);
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not remove uuid '" + uuid + "' from whitelist due to SQL error.", e);
        }
    }

    @Override
    public void addWhitelist(CIDRUtils cidr) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        try(var statement = Query.prepare("insert into `whitelisted-ranges` (`cidr_string`, `ip_start`, `ip_end`) values (?, ?, ?)")
                .append(cidr.getCidr()).append(cidr.getStartIpInt()).append(cidr.getEndIpInt())) {
            statement.execute();

        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not add cidr '" + cidr + "' to whitelist due to SQL error.", e);
        }
    }

    @Override
    public void removeWhitelist(CIDRUtils cidr) {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return;

        try(var statement = Query.prepare("delete from `whitelisted-ranges` where `cidr_string` = ?").append(cidr.getCidr())) {
            statement.execute();

        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not remove cidr '" + cidr + "' from whitelist due to SQL error.", e);
        }
    }

    @Override
    public List<UUID> getAllWhitelisted() {
        List<UUID> uuids = new ArrayList<>();

        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return uuids;

        try(var statement = Query.prepare("select uuid from `whitelisted`")) {
            statement.execute(set -> uuids.add(UUID.fromString(set.getString("uuid"))));
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not get all whitelisted players due to SQL error.", e);
        }

        return uuids;
    }

    @Override
    public List<CIDRUtils> getAllWhitelistedIps() {
        List<CIDRUtils> ips = new ArrayList<>();

        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled() || MySQL.isClosed())
            return ips;
        try(var statement = Query.prepare("select `cidr_string`, `ip_start`, `ip_end` from `whitelisted-ranges`")) {
            statement.execute(set -> {
                        try {
                            String cidrString = set.getString("cidr_string");

                            AntiVPN.getInstance().getExecutor().log("CIDR String: %s", cidrString);
                            ips.add(new CIDRUtils(cidrString));

                        } catch (UnknownHostException e) {
                            AntiVPN.getInstance().getExecutor()
                                    .logException("Could not format ip "
                                            + set.getString("cidr_string") + " into a CIDR!", e);
                        }
                    });
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not get all whitelisted ips due to SQL error.", e);
        }

        return ips;
    }

    @Override
    public void alertsState(UUID uuid, Consumer<Boolean> result) {
        if(MySQL.isClosed()) return;
        AntiVPN.getInstance().getExecutor().getThreadExecutor().execute(() -> {

            try(var statement = Query.prepare("select * from `alerts` where `uuid` = ? limit 1")
                    .append(uuid.toString())) {
                try(var set = statement.executeQuery()) {
                    result.accept(set != null && set.next() && set.getString("uuid") != null);
                }
            } catch (SQLException e) {
                AntiVPN.getInstance().getExecutor().logException("There was a problem getting alerts state for " + uuid, e);
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
                    try(var statement = Query.prepare("insert into `alerts` (`uuid`) values (?)")
                            .append(uuid.toString())) {
                        statement.execute();
                    } catch (SQLException e) {
                        AntiVPN.getInstance().getExecutor()
                                .logException("There was a problem updating alerts state for " + uuid, e);
                    }
                } //No need to insert again of already enabled
            });
            //Removing any uuid from the alerts table will disable alerts globally.
        } else {
            try(var statement = Query.prepare("delete from `alerts` where `uuid` = ?").append(uuid.toString())) {
                        statement.execute();
            } catch (SQLException e) {
                AntiVPN.getInstance().getExecutor().logException("There was a problem updating alerts state for "
                        + uuid, e);
            }
        }
    }

    @Override
    public void clearResponses() {
        if(MySQL.isClosed()) return;

        try(var statement = Query.prepare("delete from `responses`")) {
            statement.execute();
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("There was a problem clearing responses.", e);
        }
    }

    @Override
    public void init() {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled())
            return;
        AntiVPN.getInstance().getExecutor().log("Initializing H2...");
        MySQL.initH2();
        try {
            for (Version<H2VPN> version : Version.h2Versions) {
                if(version.needsUpdate(this)) {
                    version.update(this);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not complete version setup due to SQL error", e);
        }

        AntiVPN.getInstance().getExecutor().log("Creating tables...");

        //Running check for old table types to update
    }

    @Override
    public void shutdown() {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled())
            return;

        MySQL.shutdown();
    }

    public void backupDatabase() {
        File dataFolder = new File(AntiVPN.getInstance().getPluginFolder(), "databases");

        if(!dataFolder.exists() || MySQL.isClosed()) {
            return;
        }

        try {
            var connection = Query.getConn();
            if (connection == null || connection.getMetaData() == null
                    || !connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2")) {
                return;
            }
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not verify database type before H2 backup.", e);
            return;
        }

        File backupDir = new File(dataFolder, "backups");
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            AntiVPN.getInstance().getExecutor().log("Could not create backup directory");
            return;
        }

        File backupFile = new File(backupDir, "database.h2_backup_" + System.currentTimeMillis() + ".zip");
        String backupPath = backupFile.getAbsolutePath()
                .replace("\\", "/")
                .replace("'", "''");

        try (var statement = Query.prepare("BACKUP TO '" + backupPath + "'")) {
            statement.execute();
            AntiVPN.getInstance().getExecutor().log("Created H2 backup at " + backupFile.getName());
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not create H2 backup before migration.", e);
        }
    }
}
