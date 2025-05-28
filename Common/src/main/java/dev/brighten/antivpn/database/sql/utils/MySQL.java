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

    private static boolean didRetry = false;

    public static void initH2() {
        if(didRetry) {
            AntiVPN.getInstance().getExecutor().log(Level.WARNING,
                    "Already attempted to retry H2 connection, skipping.");
            return;
        }
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
                backupOldDB(dbFile, dataFolder);
                initH2();
                didRetry = true;
            } else {
                AntiVPN.getInstance().getExecutor().logException("Failed to load H2 database: " + ex.getCause().toString(), ex);
            }
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().logException("Failed to load H2 database: " + e.getMessage(), e);
            AntiVPN.getInstance().getExecutor().log(Level.INFO, "TIP: Try deleting the plugin folder and restarting your server!");
        }
    }

    private static void backupOldDB(File dbFile, File dataFolder) {
        if (dbFile.exists()) {
            try {
                // Optional: Make backup first
                File backupDir = new File(dataFolder, "backups");
                if(backupDir.mkdirs()) {
                    AntiVPN.getInstance().getExecutor().log("Created backup directory");
                } else {
                    AntiVPN.getInstance().getExecutor().log("Backup directory already exists");
                }
                File backupFile = new File(backupDir, "database.mv.db.backup_" + System.currentTimeMillis());
                Files.copy(dbFile.toPath(), backupFile.toPath());

                // Actually delete the file
                if (!dbFile.delete()) {
                    // If normal delete fails, try force delete on JVM exit
                    dbFile.deleteOnExit();
                    AntiVPN.getInstance().getExecutor().log("Could not delete database file - will try again on shutdown");
                } else {
                    AntiVPN.getInstance().getExecutor().log("Successfully deleted incompatible database file");
                }
            } catch (IOException ex) {
                AntiVPN.getInstance().getExecutor().logException("Failed to handle database file", ex);
            }
        }
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
