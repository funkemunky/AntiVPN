package dev.brighten.antivpn.database.sqllite.version.impl;

import dev.brighten.antivpn.database.sqllite.LiteDatabase;
import dev.brighten.antivpn.database.sqllite.version.Version;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class First implements Version {

    @Override
    public void update(LiteDatabase database) {
        try(Connection connection = database.connection()) {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS vpn_responses (ip TEXT, date INTEGER, response TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS whitelist (uuid TEXT, minimum NUMERIC, maximum NUMERIC)");
            statement.execute("CREATE TABLE IF NOT EXISTS alerts (uuid TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS version (version INTEGER PRIMARY KEY, updated BOOLEAN)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int versionNumber() {
        return 1;
    }
}
