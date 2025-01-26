package dev.brighten.antivpn.database.sql.utils;

import com.mysql.cj.jdbc.Driver;
import dev.brighten.antivpn.AntiVPN;
import org.h2.jdbc.JdbcConnection;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

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
        File dataFolder = new File(AntiVPN.getInstance().getPluginFolder(), "databases");
        File databaseFile = new File(dataFolder, "database");
        try {
            conn = new NonClosableConnection(new JdbcConnection("jdbc:h2:file:" +
                    databaseFile.getAbsolutePath(),
                    new Properties(), AntiVPN.getInstance().getVpnConfig().getUsername(),
                    AntiVPN.getInstance().getVpnConfig().getPassword(), false));
            conn.setAutoCommit(true);
            Query.use(conn);
            AntiVPN.getInstance().getExecutor().log("Connection to H2 has been established.");
        } catch (SQLException ex) {
            AntiVPN.getInstance().getExecutor().logException("H2 exception on initialize: " + ex.getMessage(), ex);
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
