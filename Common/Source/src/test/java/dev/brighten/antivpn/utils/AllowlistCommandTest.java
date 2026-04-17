package dev.brighten.antivpn.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.command.CommandExecutor;
import dev.brighten.antivpn.command.impl.AllowlistCommand;
import dev.brighten.antivpn.database.VPNDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AllowlistCommandTest {

    private static final UUID FUNKEMUNKY_UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Mock
    private AntiVPN antiVPN;

    @Mock
    private VPNConfig vpnConfig;

    @Mock
    private VPNDatabase database;

    @Mock
    private dev.brighten.antivpn.api.PlayerExecutor playerExecutor;

    @Mock
    private CommandExecutor commandExecutor;

    private TestVPNExecutor vpnExecutor;
    private AutoCloseable mocks;
    private AllowlistCommand command;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        vpnExecutor = new TestVPNExecutor();
        command = new AllowlistCommand();

        when(antiVPN.getVpnConfig()).thenReturn(vpnConfig);
        when(antiVPN.getExecutor()).thenReturn(vpnExecutor);
        when(antiVPN.getDatabase()).thenReturn(database);
        when(antiVPN.getPlayerExecutor()).thenReturn(playerExecutor);

        when(vpnConfig.isDatabaseEnabled()).thenReturn(true);

        setAntiVpnInstance(antiVPN);
    }

    @AfterEach
    void tearDown() throws Exception {
        MiscUtils.resetLookupEndpointsForTesting();
        setAntiVpnInstance(null);

        if (vpnExecutor != null) {
            vpnExecutor.getThreadExecutor().shutdownNow();
        }

        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void onlinePlayerIsWhitelistedWithoutLookup() {
        APIPlayer onlinePlayer = mock(APIPlayer.class);
        when(onlinePlayer.getUuid()).thenReturn(FUNKEMUNKY_UUID);
        when(playerExecutor.getPlayer("funkemunky")).thenReturn(Optional.of(onlinePlayer));

        String result = command.execute(commandExecutor, new String[] {"add", "funkemunky"});

        assertTrue(result.contains(FUNKEMUNKY_UUID.toString()));
        verify(playerExecutor).getPlayer("funkemunky");
        verify(database).addWhitelist(FUNKEMUNKY_UUID);
        verify(database, never()).removeWhitelist(FUNKEMUNKY_UUID);
    }

    @Test
    void offlinePlayerFallsBackToMojangLookupWhenPrimaryEndpointCannotBeReached() throws Exception {
        when(playerExecutor.getPlayer("funkemunky")).thenReturn(Optional.empty());

        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        AtomicInteger mojangHits = new AtomicInteger();
        HttpServer mojangServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mojangServer.createContext("/users/profiles/minecraft/funkemunky", exchange -> {
            mojangHits.incrementAndGet();
            respond(exchange);
        });
        mojangServer.start();

        try {
            MiscUtils.setLookupEndpointsForTesting(
                    "http://127.0.0.1:" + closedPort + "/mojang/uuid?name=",
                    "http://127.0.0.1:" + mojangServer.getAddress().getPort() + "/users/profiles/minecraft/"
            );

            String result = command.execute(commandExecutor, new String[] {"add", "funkemunky"});

            assertTrue(result.contains(FUNKEMUNKY_UUID.toString()));
            verify(playerExecutor).getPlayer("funkemunky");
            assertEquals(1, mojangHits.get(), "Expected Mojang lookup to be used as the fallback");
            verify(database).addWhitelist(FUNKEMUNKY_UUID);
        } finally {
            mojangServer.stop(0);
        }
    }

    private static void respond(HttpExchange exchange) throws IOException {
        byte[] bytes = "{\"id\":\"123e4567e89b12d3a456426614174000\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (exchange; OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private static void setAntiVpnInstance(AntiVPN instance) throws Exception {
        Field instanceField = AntiVPN.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, instance);
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
