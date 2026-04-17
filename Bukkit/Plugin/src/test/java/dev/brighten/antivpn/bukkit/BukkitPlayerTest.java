package dev.brighten.antivpn.bukkit;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BukkitPlayerTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();

        BukkitPlugin pluginBootstrap = mock(BukkitPlugin.class);
        JavaPlugin plugin = MockBukkit.createMockPlugin();
        when(pluginBootstrap.getPlugin()).thenReturn(plugin);
        BukkitPlugin.pluginInstance = pluginBootstrap;
    }

    @AfterEach
    void tearDown() {
        BukkitPlugin.pluginInstance = null;
        MockBukkit.unmock();
    }

    @Test
    void kickPlayerCalledFromAsyncContext_isScheduledAndExecutedOnMainThread() {
        PlayerMock player = server.addPlayer("AsyncKickPlayer");
        BukkitPlayer bukkitPlayer = new BukkitPlayer(player);

        assertTrue(player.isOnline());

        CompletableFuture<Void> asyncKick = CompletableFuture.runAsync(() -> bukkitPlayer.kickPlayer("&cBlocked!"));
        assertDoesNotThrow(() -> asyncKick.get(1, TimeUnit.SECONDS));

        assertTrue(player.isOnline(), "Kick should be deferred to the server scheduler");

        server.getScheduler().performTicks(1);

        assertFalse(player.isOnline(), "Player should be kicked when scheduled task runs");
    }
}
