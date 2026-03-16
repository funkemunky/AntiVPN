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
import dev.brighten.antivpn.database.sql.utils.Query;
import dev.brighten.antivpn.database.version.Version;
import dev.brighten.antivpn.utils.CIDRUtils;
import dev.brighten.antivpn.utils.MiscUtils;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Third implements Version<VPNDatabase> {
    @Override
    public void update(VPNDatabase database) throws DatabaseException {
        List<CIDRUtils> ipRanges = new ArrayList<>();
        List<CIDRUtils> rangesToInsert = new ArrayList<>();
        List<BigInteger[]> rangesToRemove = new ArrayList<>(); 
        try (var preparedQuery = Query.prepare("select ip_start, ip_end from `whitelisted-ranges`")) {
            preparedQuery.execute(set -> {
                BigInteger start = set.getBigDecimal("ip_start").toBigInteger();
                BigInteger end = set.getBigDecimal("ip_end").toBigInteger();

                try {
                    var range = MiscUtils.rangeToCidrs(start, end);

                    if(range.size() > 1) {
                        rangesToRemove.add(new BigInteger[]{start, end});
                        rangesToInsert.addAll(range);
                        AntiVPN.getInstance().getExecutor().log(Level.WARNING, "Found multiple CIDR ranges for whitelist range for %s, %s!", start, end);
                    } else ipRanges.addAll(range);
                } catch (UnknownHostException e) {
                    AntiVPN.getInstance().getExecutor().logException(
                            String.format("Could not convert ip range to CIDR! %s, %s", start, end), e);
                }
            });
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not get all whitelisted ranges due to SQL error.", e);
        }

        AntiVPN.getInstance().getExecutor().log("Inserting %s new ranges into database...", rangesToInsert.size());

        for (CIDRUtils cidr : rangesToInsert) {
            try(var statement = Query.prepare("insert into `whitelisted-ranges` (`cidr_string`, `ip_start`, `ip_end`) values (?, ?, ?)")
                    .append(cidr.getCidr()).append(cidr.getStartIpInt()).append(cidr.getEndIpInt())) {
                statement.execute();
            } catch (SQLException e) {
                AntiVPN.getInstance().getExecutor().logException("Could not add cidr '" + cidr + "' to whitelist due to SQL error.", e);
            }
        }

        AntiVPN.getInstance().getExecutor().log("Removing %s old ranges from database...", rangesToRemove.size());

        for (BigInteger[] range : rangesToRemove) {
            try(var statement = Query.prepare("delete from `whitelisted-ranges` where `ip_start` = ? and `ip_end` = ?")) {
                statement.append(range[0]).append(range[1]).execute();
            } catch (SQLException e) {
                AntiVPN.getInstance().getExecutor().logException("Could not remove cidr range '" + range[0] + ", " + range[1] + "' from whitelist due to SQL error.", e);
            }
        }

        AntiVPN.getInstance().getExecutor().log("Updating %s ranges to proper CIDR notation with the database", ipRanges.size());

        for (CIDRUtils cidr : ipRanges) {
            try(var statement = Query.prepare("update `whitelisted-ranges` set `cidr_string` = ? where `ip_start` = ? and `ip_end` = ?")) {
                statement.append(cidr.getCidr()).append(cidr.getStartIpInt()).append(cidr.getEndIpInt()).execute();
            } catch (SQLException e) {
                AntiVPN.getInstance().getExecutor().logException("Could not update cidr '" + cidr + "' to proper CIDR notation in whitelist due to SQL error.", e);
            }
        }

        try (var preparedStatement = Query.prepare("INSERT INTO `database_version` (`version`) VALUES (?)").append(versionNumber())) {
            preparedStatement.execute();
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException("Could not update database version to 2 due to SQL error.", e);
        }
    }

    @Override
    public int versionNumber() {
        return 2;
    }

    @Override
    public boolean needsUpdate(VPNDatabase database) {
        try (var statement = Query.prepare("select * from `database_version` where version = 2")) {
            try(var set = statement.executeQuery()) {
                return set.getFetchSize() == 0;
            }
        } catch (SQLException e) {
            return true;
        }
    }
}
