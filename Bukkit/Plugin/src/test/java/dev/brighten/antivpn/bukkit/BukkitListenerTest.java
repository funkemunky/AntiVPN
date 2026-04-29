package dev.brighten.antivpn.bukkit;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.PlayerExecutor;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.message.MessageHandler;
import dev.brighten.antivpn.message.VpnString;
import dev.brighten.antivpn.web.objects.VPNResponse;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class BukkitListenerTest {

    private ServerMock server;
    private BukkitListener listener;
    private VPNExecutor vpnExecutor;

    @BeforeEach
    public void setUp() throws Exception {
        server = MockBukkit.mock(new RecordingServerMock());
        JavaPlugin plugin = MockBukkit.loadWith(
                TestPlugin.class,
                new PluginDescriptionFile("AntiVPNTest", "1.0.0", TestPlugin.class.getName())
        );
        BukkitPlugin.pluginInstance = new BukkitPlugin(plugin);

        AntiVPN antiVPN = mock(AntiVPN.class);
        VPNConfig config = mock(VPNConfig.class);
        PlayerExecutor playerExecutor = mock(PlayerExecutor.class);
        vpnExecutor = mock(VPNExecutor.class);
        MessageHandler messageHandler = mock(MessageHandler.class);
        
        when(antiVPN.getVpnConfig()).thenReturn(config);
        when(antiVPN.getPlayerExecutor()).thenReturn(playerExecutor);
        when(antiVPN.getExecutor()).thenReturn(vpnExecutor);
        when(antiVPN.getMessageHandler()).thenReturn(messageHandler);
        
        when(playerExecutor.getPlayer(any(UUID.class))).thenReturn(Optional.empty());
        when(config.getPrefixWhitelists()).thenReturn(java.util.Collections.emptyList());
        when(config.getCountryList()).thenReturn(java.util.Collections.emptyList());
        when(config.isKickPlayers()).thenReturn(true);
        when(config.getKickMessage()).thenReturn("Blocked!");
        
        VpnString mockVpnString = mock(VpnString.class);
        when(mockVpnString.getFormattedMessage(any())).thenReturn("Blocked!");
        when(messageHandler.getString(anyString())).thenReturn(mockVpnString);
        
        when(vpnExecutor.checkIp(anyString())).thenReturn(CompletableFuture.completedFuture(
                VPNResponse.builder().success(true).proxy(false).ip("127.0.0.1")
                        .method("N/A").countryName("N/A").city("N/A").build()
        ));
        
        // Use reflection to set the private static INSTANCE field
        Field instanceField = AntiVPN.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, antiVPN);
        
        listener = new BukkitListener();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Reset the singleton
        Field instanceField = AntiVPN.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
        BukkitPlugin.pluginInstance = null;
        
        MockBukkit.unmock();
    }

    @Test
    public void testLoginEventAllowed() throws Exception {
        PlayerMock player = server.addPlayer("TestPlayer");
        InetAddress address = InetAddress.getByName("127.0.0.1");
        
        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", address);
        
        listener.onLogin(event);
        
        assertEquals(PlayerLoginEvent.Result.ALLOWED, event.getResult());
    }

    @Test
    public void testLoginEventBlocked() throws Exception {
        PlayerMock player = server.addPlayer("ProxyPlayer");
        InetAddress address = InetAddress.getByName("1.1.1.1");
        
        // Mock proxy response
        when(vpnExecutor.checkIp("1.1.1.1")).thenReturn(CompletableFuture.completedFuture(
                VPNResponse.builder().success(true).proxy(true).ip("1.1.1.1")
                        .method("N/A").countryName("N/A").countryCode("N/A").city("N/A").build()
        ));
        
        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", address);
        
        listener.onLogin(event);
        
        assertEquals(PlayerLoginEvent.Result.KICK_BANNED, event.getResult());
        assertEquals("Blocked!", net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(event.kickMessage()));
    }

    @Test
    public void testLoginPipelineProxyPlayerIsKickedWithoutErrors() throws Exception {
        PlayerMock player = server.addPlayer("PipelineProxyPlayer");
        InetAddress address = InetAddress.getByName("1.1.1.1");

        when(vpnExecutor.checkIp("1.1.1.1")).thenReturn(CompletableFuture.completedFuture(
                VPNResponse.builder().success(true).proxy(true).ip("1.1.1.1")
                        .method("N/A").countryName("N/A").countryCode("N/A").city("N/A").build()
        ));

        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", address);

        assertDoesNotThrow(() -> listener.onLogin(event));

        assertEquals(PlayerLoginEvent.Result.KICK_BANNED, event.getResult());
        assertEquals("Blocked!", net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(event.kickMessage()));
    }

    @Test
    public void testRunCommandDispatchesOnPrimaryThreadWhenCalledAsynchronously() {
        RecordingServerMock recordingServer = (RecordingServerMock) server;
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            CompletableFuture<Void> asyncCall = CompletableFuture.runAsync(
                    () -> listener.runCommand("antivpn-test &aok"),
                    executor
            );

            assertDoesNotThrow(() -> asyncCall.get(5, TimeUnit.SECONDS));
            assertFalse(recordingServer.commandDispatched(), "Command should be scheduled, not dispatched asynchronously");

            server.getScheduler().performOneTick();

            assertTrue(recordingServer.commandDispatched(), "Scheduled command should be dispatched on the next server tick");
            assertTrue(recordingServer.dispatchedOnPrimaryThread(), "Command dispatch must happen on Bukkit's primary thread");
            assertEquals("antivpn-test §aok", recordingServer.dispatchedCommand());
        } finally {
            executor.shutdownNow();
        }
    }

    public static class TestPlugin extends JavaPlugin {
    }

    private static class RecordingServerMock extends ServerMock {
        private final AtomicBoolean commandDispatched = new AtomicBoolean();
        private final AtomicBoolean dispatchedOnPrimaryThread = new AtomicBoolean();
        private final AtomicReference<String> dispatchedCommand = new AtomicReference<>();

        @Override
        public boolean dispatchCommand(@NotNull CommandSender sender, @NotNull String commandLine) {
            commandDispatched.set(true);
            dispatchedOnPrimaryThread.set(isPrimaryThread());
            dispatchedCommand.set(commandLine);
            return super.dispatchCommand(sender, commandLine);
        }

        private boolean commandDispatched() {
            return commandDispatched.get();
        }

        private boolean dispatchedOnPrimaryThread() {
            return dispatchedOnPrimaryThread.get();
        }

        private String dispatchedCommand() {
            return dispatchedCommand.get();
        }
    }
}
