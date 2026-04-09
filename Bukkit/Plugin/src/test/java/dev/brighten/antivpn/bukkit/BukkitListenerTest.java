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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class BukkitListenerTest {

    private ServerMock server;
    private BukkitListener listener;
    private AntiVPN antiVPN;
    private VPNConfig config;
    private PlayerExecutor playerExecutor;
    private VPNExecutor vpnExecutor;
    private MessageHandler messageHandler;

    @BeforeEach
    public void setUp() throws Exception {
        server = MockBukkit.mock();
        
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
        
        listener = new BukkitListener();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Reset the singleton
        Field instanceField = AntiVPN.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
        
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
                        .method("N/A").countryName("N/A").city("N/A").build()
        ));
        
        PlayerLoginEvent event = new PlayerLoginEvent(player, "localhost", address);
        
        listener.onLogin(event);
        
        assertEquals(PlayerLoginEvent.Result.KICK_BANNED, event.getResult());
        assertEquals("Blocked!", event.getKickMessage());
    }
}
