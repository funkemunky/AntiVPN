package dev.brighten.antivpn.database.sql.utils;

import lombok.SneakyThrows;

import java.sql.Connection;

public class Query {
    private static Connection conn;

    public static void use(Connection conn) {
        Query.conn = conn;
    }

    @SneakyThrows
    public static ExecutableStatement prepare(String query) {
        if (conn.isClosed()) return null;
        return new ExecutableStatement(conn.prepareStatement(query));
    }



    @SneakyThrows
    public static ExecutableStatement prepare(String query, Connection con) {
        if (con.isClosed()) return null;
        return new ExecutableStatement(con.prepareStatement(query));
    }
}
