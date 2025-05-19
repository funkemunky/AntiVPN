package dev.brighten.antivpn.database.sql.utils;

import java.sql.Connection;
import java.sql.SQLException;

public class Query {
    private static Connection conn;

    public static void use(Connection conn) {
        Query.conn = conn;
    }

    public static ExecutableStatement prepare(String query) {
        try {
            return new ExecutableStatement(conn.prepareStatement(query));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
