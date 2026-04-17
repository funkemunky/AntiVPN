package dev.brighten.antivpn.database;

import dev.brighten.antivpn.database.sql.MySqlVPN;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Testcontainers
class MySqlDatabaseIntegrationTest extends DatabaseIntegrationTestSupport {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("antivpn")
            .withUsername("testuser")
            .withPassword("testpass");

    @Test
    void mysqlDatabaseImplementsTheVpnDatabaseContract() throws Exception {
        assertTrue(MYSQL.isRunning(), "MySQL Testcontainer should be running");

        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT 1")) {
            assertTrue(resultSet.next(), "Expected a row from the MySQL container");
            assertEquals(1, resultSet.getInt(1), "Expected MySQL container to respond to SELECT 1");
        }

        var pingResult = MYSQL.execInContainer("mysqladmin", "ping", "-h", "127.0.0.1", "-ptestpass");
        assertEquals(0, pingResult.getExitCode(), "Expected mysqladmin ping to succeed inside the container");

        when(vpnConfig.getIp()).thenReturn(MYSQL.getHost());
        when(vpnConfig.getPort()).thenReturn(MYSQL.getMappedPort(3306));
        when(vpnConfig.getDatabaseName()).thenReturn(MYSQL.getDatabaseName());
        when(vpnConfig.getUsername()).thenReturn(MYSQL.getUsername());
        when(vpnConfig.getPassword()).thenReturn(MYSQL.getPassword());

        assertDatabaseContract(new MySqlVPN());
    }
}
