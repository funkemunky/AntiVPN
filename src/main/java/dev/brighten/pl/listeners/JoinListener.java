package dev.brighten.pl.listeners;

import cc.funkemunky.api.utils.Init;
import dev.brighten.pl.AntiVPN;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@Init
public class JoinListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        AntiVPN.INSTANCE.vpnHandler.checkPlayer(event.getPlayer());
    }

}
