package dev.brighten.antivpn.database;

import com.mongodb.client.MongoClients;
import dev.brighten.antivpn.database.mongo.MongoVPN;
import org.junit.jupiter.api.Test;
import org.bson.Document;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Testcontainers(disabledWithoutDocker = true)
class MongoDatabaseIntegrationTest extends DatabaseIntegrationTestSupport {

    @Container
    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:6.0.14");

    @Test
    void mongoDatabaseImplementsTheVpnDatabaseContract() throws Exception {
        assertTrue(MONGO.isRunning(), "Mongo Testcontainer should be running");

        try (var client = MongoClients.create(MONGO.getConnectionString())) {
            var response = client.getDatabase("admin").runCommand(new Document("ping", 1));
            assertEquals(1.0d, response.getDouble("ok"), "Expected Mongo container to respond to ping");
        }

        when(vpnConfig.getIp()).thenReturn(MONGO.getHost());
        when(vpnConfig.getPort()).thenReturn(MONGO.getMappedPort(27017));
        when(vpnConfig.getDatabaseName()).thenReturn("antivpn_" + UUID.randomUUID().toString().replace("-", ""));
        when(vpnConfig.mongoDatabaseURL()).thenReturn("");
        when(vpnConfig.useDatabaseCreds()).thenReturn(false);

        assertDatabaseContract(new MongoVPN());
    }
}
