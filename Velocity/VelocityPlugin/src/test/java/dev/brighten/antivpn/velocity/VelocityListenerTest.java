package dev.brighten.antivpn.velocity;

import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
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

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import java.util.logging.Logger;

import static org.mockito.Mockito.*;

public class VelocityListenerTest {

    private VelocityListener listener;
    private AntiVPN antiVPN;
    private VPNConfig config;
    private PlayerExecutor playerExecutor;
    private VPNExecutor vpnExecutor;
    private MessageHandler messageHandler;
    private VelocityPlugin velocityPlugin;

    @BeforeEach
    public void setUp() throws Exception {
        antiVPN = mock(AntiVPN.class);
        config = mock(VPNConfig.class);
        playerExecutor = mock(PlayerExecutor.class);
        vpnExecutor = mock(VPNExecutor.class);
        messageHandler = mock(MessageHandler.class);
        velocityPlugin = mock(VelocityPlugin.class);

        when(antiVPN.getVpnConfig()).thenReturn(config);
        when(antiVPN.getPlayerExecutor()).thenReturn(playerExecutor);
        when(antiVPN.getExecutor()).thenReturn(vpnExecutor);
        when(antiVPN.getMessageHandler()).thenReturn(messageHandler);

        when(velocityPlugin.getLogger()).thenReturn(Logger.getLogger("AntiVPN"));

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
                        .method("N/A").countryName("N/A").city("N/A").countryCode("N/A").build()
        ));

        // Use reflection to set the private static INSTANCE field
        Field instanceField = AntiVPN.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, antiVPN);

        Field pluginInstanceField = VelocityPlugin.class.getDeclaredField("INSTANCE");
        pluginInstanceField.setAccessible(true);
        pluginInstanceField.set(null, velocityPlugin);

        listener = new VelocityListener();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Reset the singletons
        Field instanceField = AntiVPN.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        Field pluginInstanceField = VelocityPlugin.class.getDeclaredField("INSTANCE");
        pluginInstanceField.setAccessible(true);
        pluginInstanceField.set(null, null);
    }

    @Test
    public void testLoginEventAllowed() throws Exception {
        LoginEvent event = mock(LoginEvent.class);
        Player player = mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getUsername()).thenReturn("TestPlayer");
        when(player.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));

        listener.onLogin(event);

        verify(event, never()).setResult(any());
    }

    @Test
    public void testLoginEventBlocked() throws Exception {
        LoginEvent event = mock(LoginEvent.class);
        Player player = mock(Player.class);

        when(event.getPlayer()).thenReturn(player);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getUsername()).thenReturn("ProxyPlayer");
        when(player.getRemoteAddress()).thenReturn(new InetSocketAddress("1.1.1.1", 12345));

        // Mock proxy response
        when(vpnExecutor.checkIp("1.1.1.1")).thenReturn(CompletableFuture.completedFuture(
                VPNResponse.builder().success(true).proxy(true).ip("1.1.1.1")
                        .method("N/A").countryName("N/A").city("N/A").countryCode("N/A").build()
        ));

        listener.onLogin(event);

        verify(event).setResult(any());
    }
}
