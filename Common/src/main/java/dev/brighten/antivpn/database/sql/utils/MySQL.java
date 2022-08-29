package dev.brighten.antivpn.database.sql.utils;

import dev.brighten.antivpn.AntiVPN;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySQL {
    private static Connection conn;

    public static void init() {
        try {
            if (conn == null || conn.isClosed()) {
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    Class.forName("com.mysql.jdbc.Driver");
                }
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
            AntiVPN.getInstance().getExecutor().log("Failed to load mysql: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean attemptedTwice = false;
    public static void initH2() {
        File dataFolder = new File(AntiVPN.getInstance().getPluginFolder(), "databases");
        File databaseFile = new File(dataFolder, "database");
        try {
            Constructor jdbcConnection = Class.forName("org.h2.jdbc.JdbcConnection")
                    .getConstructor(String.class, Properties.class, String.class, Object.class, boolean.class);
            conn = new NonClosableConnection((Connection)jdbcConnection.newInstance("jdbc:h2:file:" +
                            databaseFile.getAbsolutePath(),
                    new Properties(), "root", "erc5gmv-xvg5CZQ0nzw", false));
            conn.setAutoCommit(true);
            Query.use(conn);
            AntiVPN.getInstance().getExecutor().log("Connection to H2 has been established.");
        } catch (SQLException ex) {
            AntiVPN.getInstance().getExecutor().log("H2 exception on initialize");
            ex.printStackTrace();
        } catch (ClassNotFoundException ex) {
            AntiVPN.getInstance().getExecutor().log("No H2 library found!");
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            AntiVPN.getInstance().getExecutor().log("Java exception on initialize");
            e.printStackTrace();
        } catch(InvocationTargetException e) {
            if(attemptedTwice) return;
            if(e.getCause() instanceof JdbcSQLNonTransientConnectionException) {
                File[] files = dataFolder.listFiles();

                if(files == null) {
                    e.printStackTrace();
                    return;
                }

                AntiVPN.getInstance().getExecutor().log("Upgrading h2 files...");
                for (File file : files) {
                    if(file.getName().endsWith(".db")) {
                        file.delete();
                    }
                }
                initH2();
            } else e.printStackTrace();
        }
        attemptedTwice = true;
    }

    public static void use() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
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
            attemptedTwice = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isClosed() {
        if(conn == null)
            return true;

        try {
            return conn.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }
}
