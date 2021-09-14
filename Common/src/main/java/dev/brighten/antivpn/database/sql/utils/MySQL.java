package dev.brighten.antivpn.database.sql.utils;

import dev.brighten.antivpn.AntiVPN;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {
    private static Connection conn;

    public static void init() {
        try {
            if (conn == null || conn.isClosed()) {
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
                System.out.println("Connection to MySQL has been established.");
            }
        } catch (Exception e) {
            System.out.println("Failed to load mysql: " + e.getMessage());
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
