package dev.brighten.antivpn.database;

import dev.brighten.antivpn.database.local.H2VPN;
import org.junit.jupiter.api.Test;

class H2DatabaseIntegrationTest extends DatabaseIntegrationTestSupport {

    @Test
    void h2DatabaseImplementsTheVpnDatabaseContract() throws Exception {
        assertDatabaseContract(new H2VPN());
    }
}
