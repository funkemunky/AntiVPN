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

import com.mysql.cj.jdbc.Driver;
import dev.brighten.antivpn.AntiVPN;
import org.h2.jdbc.JdbcSQLFeatureNotSupportedException;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

public class MySQL {
    private static Connection conn;

    public static void init() {
        try {
            if (conn == null || conn.isClosed()) {
                DriverManager.registerDriver(new Driver());
                conn = DriverManager.getConnection("jdbc:mysql://" + AntiVPN.getInstance().getVpnConfig().getIp()
                                + ":" + AntiVPN.getInstance().getVpnConfig().getPort()
                                + "/?useSSL=true&autoReconnect=true",
                        AntiVPN.getInstance().getVpnConfig().getUsername(),
                        AntiVPN.getInstance().getVpnConfig().getPassword());
                conn.setAutoCommit(true);
                Query.use(conn);
                Query.prepare("CREATE DATABASE IF NOT EXISTS `"
                        + AntiVPN.getInstance().getVpnConfig().getDatabaseName() + "`").execute();
                Query.prepare("USE `" + AntiVPN.getInstance().getVpnConfig().getDatabaseName() + "`").execute();
                AntiVPN.getInstance().getExecutor().log("Connection to MySQL has been established.");
            }
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().logException("Failed to load mysql: " + e.getMessage(), e);
        }
    }

    public static void initH2() {
        initH2(true);
    }

    private static void initH2(boolean allowRetry) {
        File dataFolder = new File(AntiVPN.getInstance().getPluginFolder(), "databases");
        if (!dataFolder.exists() && dataFolder.mkdirs()) {
            AntiVPN.getInstance().getExecutor().log("Created database directory");
        }

        File dbFile = new File(dataFolder, "database.mv.db");

        File databaseFile = new File(dataFolder, "database");
        try {
            conn = new NonClosableConnection(new org.h2.jdbc.JdbcConnection("jdbc:h2:file:" +
                    databaseFile.getAbsolutePath(),
                    new Properties(), AntiVPN.getInstance().getVpnConfig().getUsername(),
                    AntiVPN.getInstance().getVpnConfig().getPassword(), false));
            conn.setAutoCommit(true);
            Query.use(conn);
            AntiVPN.getInstance().getExecutor().log("Connection to H2 has been established.");
        } catch (SQLException ex) {
            AntiVPN.getInstance().getExecutor().logException("H2 exception on initialize", ex);
            if(ex instanceof JdbcSQLFeatureNotSupportedException
                    || ex instanceof JdbcSQLNonTransientConnectionException) {
                AntiVPN.getInstance().getExecutor()
                        .log("H2 database file is incompatible with this version of AntiVPN. " +
                                "Backing up old database file...");
                shutdown();
                if (allowRetry && backupOldDB(dbFile, dataFolder)) {
                    initH2(false);
                } else {
                    AntiVPN.getInstance().getExecutor().log(
                            "Could not back up and remove the incompatible H2 database file automatically.");
                }
            } else {
                AntiVPN.getInstance().getExecutor().logException("Failed to load H2 database: " + ex.getCause().toString(), ex);
            }
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().logException("Failed to load H2 database: " + e.getMessage(), e);
            AntiVPN.getInstance().getExecutor().log(Level.INFO, "TIP: Try deleting the plugin folder and restarting your server!");
        }
    }

    public static boolean backupOldDB(File dbFile, File dataFolder) {
        if (!dbFile.exists()) {
            return true;
        }

        if (!dbFile.isFile()) {
            AntiVPN.getInstance().getExecutor().log("Skipping backup for non-file path: " + dbFile.getAbsolutePath());
            return false;
        }

        try {
            File backupDir = new File(dataFolder, "backups");
            if(backupDir.mkdirs()) {
                AntiVPN.getInstance().getExecutor().log("Created backup directory");
            } else if (backupDir.exists()) {
                AntiVPN.getInstance().getExecutor().log("Backup directory already exists");
            } else {
                AntiVPN.getInstance().getExecutor().log("Could not create backup directory");
                return false;
            }
            File backupFile = new File(backupDir, dbFile.getName() + ".backup_" + System.currentTimeMillis());
            Files.copy(dbFile.toPath(), backupFile.toPath());

            if (!dbFile.delete()) {
                dbFile.deleteOnExit();
                AntiVPN.getInstance().getExecutor().log("Could not delete database file - will try again on shutdown");
                return false;
            }

            AntiVPN.getInstance().getExecutor().log("Successfully deleted incompatible database file");
            return true;
        } catch (IOException ex) {
            AntiVPN.getInstance().getExecutor().logException("Failed to handle database file", ex);
        }

        return false;
    }

    public static void use() {
        try {
            init();
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    public static void shutdown() {
        try {
            if(conn != null && !conn.isClosed()) {
                if(conn instanceof NonClosableConnection) {
                    ((NonClosableConnection)conn).shutdown();
                } else conn.close();
                conn = null;
            }
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    public static boolean isClosed() {
        if(conn == null)
            return true;

        try {
            return conn.isClosed();
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
            return true;
        }
    }
}
