package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.PlayerExecutor;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.message.MessageHandler;
import dev.brighten.antivpn.message.VpnString;
import dev.brighten.antivpn.web.objects.VPNResponse;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

public class BungeeListenerTest {

    private BungeeListener listener;
    private VPNExecutor vpnExecutor;

    @BeforeEach
    public void setUp() throws Exception {
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

        listener = new BungeeListener();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Reset the singleton
        Field instanceField = AntiVPN.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    public void testPreLoginEventAllowed()  {
        PreLoginEvent event = mock(PreLoginEvent.class);
        PendingConnection connection = mock(PendingConnection.class);

        when(event.getConnection()).thenReturn(connection);
        when(connection.getUniqueId()).thenReturn(UUID.randomUUID());
        when(connection.getName()).thenReturn("TestPlayer");
        when(connection.getSocketAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));

        listener.onListener(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void testPreLoginEventBlocked() {
        PreLoginEvent event = mock(PreLoginEvent.class);
        PendingConnection connection = mock(PendingConnection.class);

        UUID uuid = UUID.randomUUID();
        when(event.getConnection()).thenReturn(connection);
        when(connection.getUniqueId()).thenReturn(uuid);
        when(connection.getName()).thenReturn("ProxyPlayer");
        when(connection.getSocketAddress()).thenReturn(new InetSocketAddress("1.1.1.1", 12345));

        // Mock proxy response
        when(vpnExecutor.checkIp("1.1.1.1")).thenReturn(CompletableFuture.completedFuture(
                VPNResponse.builder().success(true).proxy(true).ip("1.1.1.1")
                        .method("N/A").countryName("N/A").countryCode("N/A").city("N/A").build()
        ));

        listener.onListener(event);

        verify(event).setCancelled(true);
        verify(event).setReason(any());
    }
}
