package dev.brighten.antivpn.database;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.database.sqllite.version.Version;
import dev.brighten.antivpn.web.objects.VPNResponse;
import org.intellij.lang.annotations.Language;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"unused", "SqlSourceToSinkFlow"})
public interface VPNDatabase {
    Optional<VPNResponse> getStoredResponse(String ip);

    void cacheResponse(VPNResponse toCache);

    void deleteResponse(String ip);

    void clearResponses();

    boolean isWhitelisted(UUID uuid);

    boolean isWhitelisted(String ip);

    void addWhitelist(UUID uuid);

    void addWhitelist(String cidr);

    void removeWhitelist(UUID uuid);

    void removeWhitelist(String cidr);

    boolean getAlertsState(UUID uuid);

    void updateAlertsState(UUID uuid, boolean state);

    void init();

    default void setupTable() throws SQLException {
        try(Connection connection = connection()) {
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS vpn_responses (ip TEXT, response TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS whitelist (uuid TEXT, minimum NUMERIC, maximum NUMERIC)");
            statement.execute("CREATE TABLE IF NOT EXISTS alerts (uuid TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS version (version INTEGER PRIMARY KEY, updated BOOLEAN)");

            // Run through updates
            for (Version version : Version.versions) {
                try(ResultSet result = query("SELECT * FROM version WHERE version = ?",
                        version.versionNumber())) {
                    if(!result.next()) {
                        version.update(this);
                        statement("INSERT INTO version (version, updated) VALUES (?, ?)",
                                version.versionNumber(), true);
                    }
                } catch (SQLException e) {
                    AntiVPN.getInstance().getExecutor().logException(e);
                }
            }
        }
    }

    default ResultSet query(@Language("SQL") String query, Object... args) throws SQLException {
        try(Connection connection = connection()) {
            PreparedStatement pstmt = connection.prepareStatement(query);
            for (int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }

            return pstmt.executeQuery();
        }
    }

    default void statement(@Language("SQL") String query, Object... args) throws SQLException {
        try(Connection connection = connection()) {
            PreparedStatement pstmt = connection.prepareStatement(query);
            for (int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }

            pstmt.execute();
        }
    }
    Connection connection();

    void shutdown();
}
