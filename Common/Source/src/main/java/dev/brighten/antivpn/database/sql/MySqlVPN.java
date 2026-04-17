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

import java.util.concurrent.TimeUnit;

public class MySqlVPN extends H2VPN {

    public MySqlVPN() {
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
    public void init() {
        if (!AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled())
            return;
        AntiVPN.getInstance().getExecutor().log("Initializing MySQL...");
        MySQL.init();

        AntiVPN.getInstance().getExecutor().log("Checking for updates...");

        //Running check for old table types to update
        try {
            for (Version<MySqlVPN> version : Version.mysqlVersions) {
                if(version.needsUpdate(this)) {
                    version.update(this);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not complete version setup due to SQL error", e);
        }
    }
}
