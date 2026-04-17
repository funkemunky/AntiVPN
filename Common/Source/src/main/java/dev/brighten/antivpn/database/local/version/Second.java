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

package dev.brighten.antivpn.database.local.version;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.database.DatabaseException;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.local.H2VPN;
import dev.brighten.antivpn.database.sql.MySqlVPN;
import dev.brighten.antivpn.database.sql.utils.ExecutableStatement;
import dev.brighten.antivpn.database.sql.utils.Query;
import dev.brighten.antivpn.database.version.Version;
import dev.brighten.antivpn.utils.CIDRUtils;
import dev.brighten.antivpn.utils.MiscUtils;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Second extends First implements Version<VPNDatabase> {
    private final List<AutoCloseable> toClose = new ArrayList<>();

    @Override
    public void update(VPNDatabase database) throws DatabaseException {
        if(database instanceof H2VPN h2VPN && !(database instanceof MySqlVPN)) {
            h2VPN.backupDatabase();
        }
        List<String> whitelistedIps = new ArrayList<>();

        try (var statement = Query.prepare("SELECT * FROM `whitelisted-ips`")) {
            try(var set = statement.executeQuery()) {
                while (set.next()) {
                    whitelistedIps.add(set.getString("ip"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not get whitelisted ips from database!", e);
        }

        try {
            closeOnEnd(Query.prepare("CREATE TABLE IF NOT EXISTS `whitelisted-ranges` " +
                    "(id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "cidr_string VARCHAR(45), " +
                    "ip_start BIGINT NOT NULL, " +
                    "ip_end BIGINT NOT NULL)"))
                    .execute();
            createIndexIfAbsent("whitelisted-ranges", "idx_ip_range", "ip_start, ip_end");

            var cidrs = whitelistedIps.stream().map(ip -> {
                try {
                    return new CIDRUtils(ip + "/32");
                } catch (UnknownHostException e) {
                    throw new RuntimeException("Could not format ip " + ip + " into a CIDR!", e);
                }
            }).toList();
            var insertStatement = Query.prepare("INSERT INTO `whitelisted-ranges` (`cidr_string`, `ip_start`, `ip_end`) VALUES (?, ?, ?)");
            for (CIDRUtils cidr : cidrs) {
                insertStatement = insertStatement
                        .append(cidr.toString())
                        .append(cidr.getStartIpInt())
                        .append(cidr.getEndIpInt())
                        .addBatch();
            }

            int[] updateCounts = insertStatement.executeBatch();

            for (int updateCount : updateCounts) {
                if(updateCount == 0) {
                    throw new RuntimeException("Could not insert a CIDR from previous whitelisted lists, attempted to restore previous database!");
                }
            }

            dropIndexIfPresent("whitelisted-ips", "ip_1");
            dropIndexIfPresent("whitelisted-ips", "whitelisted_ips_ip_1");
            closeOnEnd(Query.prepare("DROP TABLE `whitelisted-ips`")).execute();
            closeOnEnd(Query.prepare("INSERT INTO `database_version` (`version`) VALUES (?)").append(versionNumber())).execute();
        } catch (Throwable e) {
            AntiVPN.getInstance().getExecutor().log("Failed to update database to version 1: " + e.getMessage());
            try {
                rollback(whitelistedIps);
            } catch (SQLException ex) {
                throw new DatabaseException("Failed to rollback database!", e);
            }
            throw new DatabaseException("Failed to update to version one, rolling back database!", e);
        } finally {
            MiscUtils.close(toClose.toArray(AutoCloseable[]::new));
            toClose.clear();
        }

    }

    private ExecutableStatement closeOnEnd(ExecutableStatement statement) {
        toClose.add(statement);
        return statement;
    }

    private void rollback(List<String> ipAddresses) throws SQLException {
        AntiVPN.getInstance().getExecutor().log("Rolling back to version 0...");
        dropIndexIfPresent("whitelisted-ranges", "idx_ip_range");
        try(var statement = Query.prepare("DROP TABLE `whitelisted-ranges`")) {
            statement.execute();
        }

        try(var statement = Query.prepare("DELETE FROM `database_version` WHERE version = ?").append(versionNumber())) {
            statement.execute();
        }

        try(var statement = Query.prepare("CREATE TABLE IF NOT EXISTS `whitelisted-ips` (`ip` VARCHAR(45) NOT NULL)")) {
            statement.execute();
        }

        createIndexIfAbsent("whitelisted-ips", "whitelisted_ips_ip_1", "`ip`");

        try(var statement = Query.prepare("DELETE FROM `whitelisted-ips`")) {
            statement.execute();
        }

        try(var statement = Query.prepare("INSERT INTO `whitelisted-ips` (`ip`) VALUES (?)")) {
            for (String ip : ipAddresses) {
                statement.append(ip);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    @Override
    public int versionNumber() {
        return 1;
    }

    @Override
    public boolean needsUpdate(VPNDatabase database) {
        try (var statement = Query.prepare("select * from `database_version` where version = 1")) {
           try(var set = statement.executeQuery()) {
               return !set.next();
           }
        } catch (SQLException e) {
            return true;
        }
    }
}
