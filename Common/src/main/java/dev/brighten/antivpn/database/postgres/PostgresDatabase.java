package dev.brighten.antivpn.database.postgres;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.database.sqllite.LiteDatabase;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PostgresDatabase extends LiteDatabase {

    @Override
    public void init() {

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
            return;
        }
        String databaseName = AntiVPN.getInstance().getVpnConfig().getDatabaseName();
        String ipAddress = AntiVPN.getInstance().getVpnConfig().getIp();
        int port = AntiVPN.getInstance().getVpnConfig().getPort();
        String username = AntiVPN.getInstance().getVpnConfig().getUsername();
        String password = AntiVPN.getInstance().getVpnConfig().getPassword();

        String url = String.format("jdbc:postgresql://%s:%s/%s", ipAddress, port, databaseName);

        Properties properties = new Properties();

        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("ssl", "true");

        try {
            connection = DriverManager.getConnection(url, properties);
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }

    }
}
