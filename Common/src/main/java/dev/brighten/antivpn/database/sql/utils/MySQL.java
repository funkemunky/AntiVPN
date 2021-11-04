package dev.brighten.antivpn.database.sql.utils;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.MiscUtils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {
    private static Connection conn;

    public static void init() {
        try {
            if (conn == null || conn.isClosed()) {
                File mysqlLib = new File(AntiVPN.getInstance().getPluginFolder(), "mysqllib.jar");

                if(!mysqlLib.exists()) {
                    AntiVPN.getInstance().getExecutor().log("Downloading mysqllib.jar...");
                    MiscUtils.download(mysqlLib, "https://nexus.funkemunky.cc/content/repositories/releases" +
                            "/mysql/mysql-connector-java/8.0.22/mysql-connector-java-8.0.22.jar");
                }
                MiscUtils.injectURL(mysqlLib.toURI().toURL());
                Class.forName("com.mysql.jdbc.Driver");
                conn = DriverManager.getConnection("jdbc:mysql://" + AntiVPN.getInstance().getConfig().getIp()
                                + ":" + AntiVPN.getInstance().getConfig().getPort()
                                + "/?useSSL=true&autoReconnect=true",
                        AntiVPN.getInstance().getConfig().getUsername(),
                        AntiVPN.getInstance().getConfig().getPassword());
                conn.setAutoCommit(true);
                Query.use(conn);
                Query.prepare("CREATE DATABASE IF NOT EXISTS `"
                        + AntiVPN.getInstance().getConfig().getDatabaseName() + "`").execute();
                Query.prepare("USE `" + AntiVPN.getInstance().getConfig().getDatabaseName() + "`").execute();
                AntiVPN.getInstance().getExecutor().log("Connection to MySQL has been established.");
            }
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().log("Failed to load mysql: " + e.getMessage());
            e.printStackTrace();
        }
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
                conn.close();
                conn = null;
            }
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
