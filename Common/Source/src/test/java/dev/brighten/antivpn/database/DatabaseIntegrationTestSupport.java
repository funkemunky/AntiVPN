package dev.brighten.antivpn.database;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.database.sql.utils.MySQL;
import dev.brighten.antivpn.utils.CIDRUtils;
import dev.brighten.antivpn.web.objects.VPNResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

abstract class DatabaseIntegrationTestSupport {

    @TempDir
    Path pluginFolder;

    @Mock
    protected AntiVPN antiVPN;

    @Mock
    protected VPNConfig vpnConfig;

    protected TestVPNExecutor vpnExecutor;
    private AutoCloseable mocks;
    private final AtomicReference<VPNDatabase> activeDatabase = new AtomicReference<>();

    @BeforeEach
    void setUpBase() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        vpnExecutor = new TestVPNExecutor();

        when(antiVPN.getVpnConfig()).thenReturn(vpnConfig);
        when(antiVPN.getExecutor()).thenReturn(vpnExecutor);
        when(antiVPN.getPluginFolder()).thenReturn(pluginFolder.toFile());
        when(antiVPN.getDatabase()).thenAnswer(invocation -> activeDatabase.get());

        lenient().when(vpnConfig.isDatabaseEnabled()).thenReturn(true);
        lenient().when(vpnConfig.cachedResults()).thenReturn(true);
        lenient().when(vpnConfig.getUsername()).thenReturn("testuser");
        lenient().when(vpnConfig.getPassword()).thenReturn("testpass");
        lenient().when(vpnConfig.getDatabaseName()).thenReturn("antivpn");
        lenient().when(vpnConfig.getIp()).thenReturn("127.0.0.1");
        lenient().when(vpnConfig.getPort()).thenReturn(-1);
        lenient().when(vpnConfig.mongoDatabaseURL()).thenReturn("");
        lenient().when(vpnConfig.useDatabaseCreds()).thenReturn(false);

        setAntiVpnInstance(antiVPN);
    }

    @AfterEach
    void tearDownBase() throws Exception {
        VPNDatabase database = activeDatabase.getAndSet(null);
        if (database != null) {
            database.shutdown();
        }

        MySQL.shutdown();
        if (vpnExecutor != null) {
            vpnExecutor.getThreadExecutor().shutdownNow();
            vpnExecutor.getThreadExecutor().awaitTermination(5, TimeUnit.SECONDS);
        }

        setAntiVpnInstance(null);

        if (mocks != null) {
            mocks.close();
        }
    }

    protected void registerDatabase(VPNDatabase database) {
        activeDatabase.set(database);
    }

    protected void assertDatabaseContract(VPNDatabase database) throws Exception {
        registerDatabase(database);
        database.init();

        VPNResponse response = VPNResponse.builder()
                .ip("1.2.3.4")
                .asn("AS123")
                .countryName("United States")
                .countryCode("US")
                .city("New York")
                .proxy(true)
                .cached(true)
                .success(true)
                .build();

        database.cacheResponse(response);

        Optional<VPNResponse> storedResponse = awaitStoredResponse(database, response.getIp());
        assertTrue(storedResponse.isPresent(), "Expected cached response to be stored");
        assertEquals("AS123", storedResponse.get().getAsn());
        assertTrue(storedResponse.get().isProxy());

        database.deleteResponse(response.getIp());
        awaitCondition(() -> database.getStoredResponse(response.getIp()).isEmpty(),
                "Expected cached response to be deleted");

        database.cacheResponse(response);
        awaitCondition(() -> database.getStoredResponse(response.getIp()).isPresent(),
                "Expected cached response to be restored");

        UUID uuid = UUID.randomUUID();
        assertFalse(database.isWhitelisted(uuid));
        database.addWhitelist(uuid);
        awaitCondition(() -> database.isWhitelisted(uuid), "Expected UUID whitelist entry to exist");
        List<UUID> whitelisted = database.getAllWhitelisted();
        assertTrue(whitelisted.contains(uuid));
        database.removeWhitelist(uuid);
        awaitCondition(() -> !database.isWhitelisted(uuid), "Expected UUID whitelist entry to be removed");

        CIDRUtils cidr = new CIDRUtils("192.168.1.0/24");
        assertFalse(database.isWhitelisted(cidr));
        database.addWhitelist(cidr);
        awaitCondition(() -> database.isWhitelisted(cidr), "Expected CIDR whitelist entry to exist");
        List<CIDRUtils> whitelistedIps = database.getAllWhitelistedIps();
        assertTrue(whitelistedIps.stream().anyMatch(entry -> entry.getCidr().equals(cidr.getCidr())));
        database.removeWhitelist(cidr);
        awaitCondition(() -> !database.isWhitelisted(cidr), "Expected CIDR whitelist entry to be removed");

        database.updateAlertsState(uuid, true);
        awaitCondition(() -> awaitAlertsState(database, uuid), "Expected alerts to be enabled");
        database.updateAlertsState(uuid, false);
        awaitCondition(() -> !awaitAlertsState(database, uuid), "Expected alerts to be disabled");

        database.clearResponses();
        awaitCondition(() -> database.getStoredResponse(response.getIp()).isEmpty(),
                "Expected cached responses to be cleared");
    }

    private Optional<VPNResponse> awaitStoredResponse(VPNDatabase database, String ip) throws InterruptedException {
        AtomicReference<Optional<VPNResponse>> result = new AtomicReference<>(Optional.empty());
        awaitCondition(() -> {
            Optional<VPNResponse> response = database.getStoredResponse(ip);
            result.set(response);
            return response.isPresent();
        }, "Timed out waiting for cached response");
        return result.get();
    }

    private boolean awaitAlertsState(VPNDatabase database, UUID uuid) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        database.alertsState(uuid, enabled -> {
            result.set(enabled);
            latch.countDown();
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS), "Timed out waiting for alerts state callback");
        return result.get();
    }

    protected void awaitCondition(CheckedBooleanSupplier condition, String failureMessage) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
            } catch (Exception e) {
                fail(e.getMessage(), e);
                return;
            }
            Thread.sleep(100);
        }
        fail(failureMessage);
    }

    private static void setAntiVpnInstance(AntiVPN instance) throws Exception {
        Field instanceField = AntiVPN.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, instance);
    }

    @FunctionalInterface
    protected interface CheckedBooleanSupplier {
        boolean getAsBoolean() throws Exception;
    }

    protected static final class TestVPNExecutor extends VPNExecutor {
        @Override
        public void registerListeners() {}

        @Override
        public void log(Level level, String log, Object... objects) {}

        @Override
        public void log(String log, Object... objects) {}

        @Override
        public void logException(String message, Throwable ex) {}

        @Override
        public void runCommand(String command) {}

        @Override
        public void disablePlugin() {}
    }
}
