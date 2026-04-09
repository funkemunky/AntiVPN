package dev.brighten.antivpn.sponge;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.PlayerExecutor;
import dev.brighten.antivpn.api.VPNConfig;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.message.MessageHandler;
import dev.brighten.antivpn.message.VpnString;
import dev.brighten.antivpn.web.objects.VPNResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.profile.GameProfile;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

public class SpongeListenerTest {

    private SpongeListener listener;
    private AntiVPN antiVPN;
    private VPNConfig config;
    private PlayerExecutor playerExecutor;
    private VPNExecutor vpnExecutor;
    private MessageHandler messageHandler;

    @BeforeEach
    public void setUp() throws Exception {
        antiVPN = mock(AntiVPN.class);
        config = mock(VPNConfig.class);
        playerExecutor = mock(PlayerExecutor.class);
        vpnExecutor = mock(VPNExecutor.class);
        messageHandler = mock(MessageHandler.class);

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

        listener = new SpongeListener();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Reset the singleton
        Field instanceField = AntiVPN.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    public void testLoginEventAllowed() throws Exception {
        ServerSideConnectionEvent.Login event = mock(ServerSideConnectionEvent.Login.class);
        GameProfile profile = mock(GameProfile.class);
        ServerSideConnection connection = mock(ServerSideConnection.class);

        when(event.profile()).thenReturn(profile);
        when(event.connection()).thenReturn(connection);
        when(profile.uuid()).thenReturn(UUID.randomUUID());
        when(profile.name()).thenReturn(Optional.of("TestPlayer"));
        when(connection.address()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));

        listener.onJoin(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    public void testLoginEventBlocked() throws Exception {
        ServerSideConnectionEvent.Login event = mock(ServerSideConnectionEvent.Login.class);
        GameProfile profile = mock(GameProfile.class);
        ServerSideConnection connection = mock(ServerSideConnection.class);

        when(event.profile()).thenReturn(profile);
        when(event.connection()).thenReturn(connection);
        when(profile.uuid()).thenReturn(UUID.randomUUID());
        when(profile.name()).thenReturn(Optional.of("ProxyPlayer"));
        when(connection.address()).thenReturn(new InetSocketAddress("1.1.1.1", 12345));

        // Mock proxy response
        when(vpnExecutor.checkIp("1.1.1.1")).thenReturn(CompletableFuture.completedFuture(
                VPNResponse.builder().success(true).proxy(true).ip("1.1.1.1")
                        .method("N/A").countryName("N/A").city("N/A").build()
        ));

        listener.onJoin(event);

        verify(event).setCancelled(true);
        verify(event).setMessage(any());
    }
}
