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

package dev.brighten.antivpn.database.sql.utils;

import lombok.Getter;
import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.SQLException;

public class Query {
    @Getter
    private static Connection conn;

    public static void use(Connection conn) {
        Query.conn = conn;
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    public static ExecutableStatement prepare(@Language("SQL") String sql) throws SQLException {
        return new ExecutableStatement(conn.prepareStatement(sql));
    }


}
