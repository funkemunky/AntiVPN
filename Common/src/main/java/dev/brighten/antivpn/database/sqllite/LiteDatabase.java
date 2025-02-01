package dev.brighten.antivpn.database.sqllite;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.sqllite.version.Version;
import dev.brighten.antivpn.utils.CIDRUtils;
import dev.brighten.antivpn.utils.IpUtils;
import dev.brighten.antivpn.utils.StringUtil;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.web.objects.VPNResponse;
import org.intellij.lang.annotations.Language;

import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class LiteDatabase implements VPNDatabase {

    private Connection connection;

    @Override
    public Optional<VPNResponse> getStoredResponse(String ip) {
        return Optional.empty();
    }

    @Override
    public void cacheResponse(VPNResponse toCache) {
        String jsonResponse;
        try {
            jsonResponse = toCache.toJson().toString();
        } catch (JSONException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
            return;
        }

        String hashedIp = StringUtil.getHash(toCache.getIp());

        try {
            statement("INSERT INTO vpn_responses (ip, response) VALUES (?, ?)", hashedIp, jsonResponse);
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    @Override
    public void deleteResponse(String ip) {
        String hashedIp = StringUtil.getHash(ip);

        try {
            statement("DELETE FROM vpn_responses WHERE ip = ?", hashedIp);
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    @Override
    public void clearResponses() {
        try {
            statement("DELETE FROM vpn_responses");
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    @Override
    public boolean isWhitelisted(UUID uuid) {
        try(ResultSet result = query("SELECT * FROM whitelist WHERE uuid = ?", uuid.toString())) {
            return result.next();
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
        return false;
    }

    @Override
    public boolean isWhitelisted(String ip) {
        CIDRUtils cidr;
        BigDecimal start, end;
        try {
            cidr = new CIDRUtils(ip);

            start = IpUtils.getIpDecimal(cidr.getStartAddress().getHostAddress()).orElseThrow();
            end = IpUtils.getIpDecimal(cidr.getEndAddress().getHostAddress()).orElseThrow();
        } catch (UnknownHostException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
            return false;
        }

        try(ResultSet result = query("SELECT * FROM whitelist " +
                "WHERE minimum >= ? AND maximum <= ?", start, end)) {
            return result.next();
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
        return false;
    }

    @Override
    public void addWhitelist(UUID uuid) {
        try {
            statement("INSERT INTO whitelist (uuid) VALUES (?)", uuid.toString());
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    @Override
    public void addWhitelist(String cidr) {
        CIDRUtils cidrUtils;
        BigDecimal start, end;
        try {
            cidrUtils = new CIDRUtils(cidr);

            start = IpUtils.getIpDecimal(cidrUtils.getStartAddress().getHostAddress()).orElseThrow();
            end = IpUtils.getIpDecimal(cidrUtils.getEndAddress().getHostAddress()).orElseThrow();
        } catch (UnknownHostException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
            return;
        }

        try {
            statement("INSERT INTO whitelist (minimum, maximum) VALUES (?, ?)", start, end);
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    @Override
    public void removeWhitelist(UUID uuid) {
        try {
            statement("DELETE FROM whitelist WHERE uuid = ?", uuid.toString());
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    @Override
    public void removeWhitelist(String cidr) {
        CIDRUtils cidrUtils;
        BigDecimal start, end;
        try {
            cidrUtils = new CIDRUtils(cidr);

            start = IpUtils.getIpDecimal(cidrUtils.getStartAddress().getHostAddress()).orElseThrow();
            end = IpUtils.getIpDecimal(cidrUtils.getEndAddress().getHostAddress()).orElseThrow();
        } catch (UnknownHostException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
            return;
        }

        try {
            statement("DELETE FROM whitelist WHERE minimum = ? AND maximum = ?", start, end);
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    @Override
    public boolean getAlertsState(UUID uuid) {
        try(ResultSet result = query("SELECT * FROM alerts WHERE uuid = ?", uuid.toString())) {
            return result.next();
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
        return false;
    }

    @Override
    public void updateAlertsState(UUID uuid, boolean state) {
        try {
            statement("INSERT INTO alerts (uuid, STATE) VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET STATE = ?",
                    uuid.toString(), state, state);
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    @Override
    public void init() {
        String url = "jdbc:sqlite:./plugins/AntiVPN/database.db";

        try {
            Connection connection = DriverManager.getConnection(url);
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS vpn_responses (ip TEXT, response TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS whitelist (uuid TEXT, NUMBER minimum, NUMBER maximum)");
            statement.execute("CREATE TABLE IF NOT EXISTS alerts (uuid TEXT)");
            statement.execute("CREATE TABLE IF NOT EXISTS version (PRIMARY KEY(version) NUMBER, updated STATE)");

            this.connection = connection;

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
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    @Override
    public void shutdown() {
        try {
            connection.close();
        } catch (SQLException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    private ResultSet query(@Language("SQL") String query, Object... args) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(query);
        for (int i = 0; i < args.length; i++) {
            pstmt.setObject(i + 1, args[i]);
        }

        return pstmt.executeQuery();
    }

    public void statement(@Language("SQL") String query, Object... args) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement(query);
        for (int i = 0; i < args.length; i++) {
            pstmt.setObject(i + 1, args[i]);
        }

        pstmt.execute();
    }
}
