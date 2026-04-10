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

package dev.brighten.antivpn.database.sql;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.database.local.H2VPN;
import dev.brighten.antivpn.database.sql.utils.MySQL;
import dev.brighten.antivpn.database.version.Version;

public class PostgreSqlVPN extends H2VPN {

    @Override
    public void init() {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled()) {
            return;
        }

        AntiVPN.getInstance().getExecutor().log("Initializing PostgreSQL...");
        MySQL.initPostgreSql();

        AntiVPN.getInstance().getExecutor().log("Checking for updates...");

        try {
            for (Version<PostgreSqlVPN> version : Version.postgresVersions) {
                if (version.needsUpdate(this)) {
                    version.update(this);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not complete version setup due to SQL error", e);
        }
    }
}
