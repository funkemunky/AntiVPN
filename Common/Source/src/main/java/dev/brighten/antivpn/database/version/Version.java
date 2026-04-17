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

package dev.brighten.antivpn.database.version;

import dev.brighten.antivpn.database.DatabaseException;
import dev.brighten.antivpn.database.local.H2VPN;
import dev.brighten.antivpn.database.local.version.First;
import dev.brighten.antivpn.database.local.version.Second;
import dev.brighten.antivpn.database.local.version.Third;
import dev.brighten.antivpn.database.mongo.MongoVPN;
import dev.brighten.antivpn.database.mongo.version.MongoFirst;
import dev.brighten.antivpn.database.mongo.version.MongoSecond;
import dev.brighten.antivpn.database.mongo.version.MongoThird;
import dev.brighten.antivpn.database.sql.MySqlVPN;
import dev.brighten.antivpn.database.sql.version.MySQLFirst;


public interface Version<DB> {
    void update(DB database) throws DatabaseException;
    int versionNumber();
    boolean needsUpdate(DB database);

    Version<MongoVPN>[] mongoDbVersions = new Version[] {new MongoFirst(), new MongoSecond(), new MongoThird()};
    Version<MySqlVPN>[] mysqlVersions = new Version[] {new MySQLFirst(), new Second(), new Third()};
    Version<H2VPN>[] h2Versions = new Version[] {new First(), new Second(), new Third()};
}