package dev.brighten.antivpn.database;

import dev.brighten.antivpn.database.sql.PostgreSqlVPN;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlDatabaseIntegrationTest extends DatabaseIntegrationTestSupport {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.3")
            .withDatabaseName("antivpn")
            .withUsername("testuser")
            .withPassword("testpass");

    @Test
    void postgreSqlDatabaseImplementsTheVpnDatabaseContract() throws Exception {
        assertTrue(POSTGRES.isRunning(), "PostgreSQL Testcontainer should be running");

        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT 1")) {
            assertTrue(resultSet.next(), "Expected a row from the PostgreSQL container");
            assertEquals(1, resultSet.getInt(1), "Expected PostgreSQL container to respond to SELECT 1");
        }

        when(vpnConfig.getIp()).thenReturn(POSTGRES.getHost());
        when(vpnConfig.getPort()).thenReturn(POSTGRES.getMappedPort(5432));
        when(vpnConfig.getDatabaseName()).thenReturn(POSTGRES.getDatabaseName());
        when(vpnConfig.getUsername()).thenReturn(POSTGRES.getUsername());
        when(vpnConfig.getPassword()).thenReturn(POSTGRES.getPassword());

        assertDatabaseContract(new PostgreSqlVPN());
    }
}
