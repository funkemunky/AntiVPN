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
import dev.brighten.antivpn.database.sql.utils.ExecutableStatement;
import dev.brighten.antivpn.database.sql.utils.Query;
import dev.brighten.antivpn.database.version.Version;
import dev.brighten.antivpn.utils.MiscUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class First implements Version<VPNDatabase> {

    private final List<AutoCloseable> toClose = new ArrayList<>();
    @Override
    public void update(VPNDatabase database) throws DatabaseException {
        try {
            closeOnEnd(Query.prepare("create table if not exists `whitelisted` (`uuid` varchar(36) not null)"))
                    .execute();
            closeOnEnd(Query.prepare("create table if not exists `whitelisted-ips` (`ip` varchar(45) not null)"))
                    .execute();
            closeOnEnd(Query
                    .prepare("create table if not exists `responses` (`ip` varchar(45) not null, `asn` varchar(12),"
                    + "`countryName` text, `countryCode` varchar(10), `city` text, `timeZone` varchar(64), "
                    + "`method` varchar(32), `isp` text, `proxy` boolean, `cached` boolean, `inserted` timestamp,"
                    + "`latitude` double, `longitude` double)")).execute();
            closeOnEnd(Query.prepare("create table if not exists `alerts` (`uuid` varchar(36) not null)"))
                    .execute();
            closeOnEnd(Query.prepare("create table if not exists `database_version` (`version` int)")).execute();
            closeOnEnd(Query.prepare("insert into `database_version` (`version`) values (?)")
                    .append(versionNumber())).execute();

            AntiVPN.getInstance().getExecutor().log("Creating indexes...");
            createIndexIfAbsent("whitelisted", "uuid_1", "`uuid`");
            createIndexIfAbsent("responses", "ip_1", "`ip`");
            createIndexIfAbsent("responses", "proxy_1", "`proxy`");
            createIndexIfAbsent("responses", "inserted_1", "`inserted`");
            createIndexIfAbsent("whitelisted-ips", "ip_1", "`ip`");
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update database", e);
        } finally {
            MiscUtils.close(toClose.toArray(AutoCloseable[]::new));
            toClose.clear();
        }
    }

    private ExecutableStatement closeOnEnd(ExecutableStatement statement) {
        toClose.add(statement);
        return statement;
    }

    protected void createIndexIfAbsent(String tableName, String indexName, String columnList) throws SQLException {
        if (hasIndex(tableName, indexName)) {
            return;
        }

        closeOnEnd(Query.prepare(String.format(
                "create index `%s` on `%s` (%s)",
                indexName,
                tableName,
                columnList
        ))).execute();
    }

    protected void dropIndexIfPresent(String tableName, String indexName) throws SQLException {
        if (!hasIndex(tableName, indexName)) {
            return;
        }

        closeOnEnd(Query.prepare(String.format(
                "drop index `%s` on `%s`",
                indexName,
                tableName
        ))).execute();
    }

    protected boolean hasIndex(String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = Query.getConn().getMetaData();

        try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (indexes.next()) {
                String existingIndexName = indexes.getString("INDEX_NAME");

                if (existingIndexName != null && existingIndexName.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public int versionNumber() {
        return 0;
    }

    @Override
    public boolean needsUpdate(VPNDatabase database) {
        try(var statement = Query.prepare("select * from `database_version` where version = 0")) {
            try(ResultSet set =  statement.executeQuery()) {
                return !set.next();
            }
        } catch (SQLException e) {
            return true;
        }

    }
}
