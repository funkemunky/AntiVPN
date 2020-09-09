package dev.brighten.pl.listeners;

import cc.funkemunky.api.utils.Init;
import cc.funkemunky.api.utils.Tuple;
import dev.brighten.pl.AntiVPN;
import dev.brighten.pl.config.Config;
import dev.brighten.pl.data.UserData;
import dev.brighten.pl.utils.StringUtils;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

@Init
public class JoinListener implements Listener {


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onEvent(AsyncPlayerPreLoginEvent event) {
        AntiVPN.INSTANCE.vpnHandler.checkPlayer(event.getUniqueId(), event.getAddress().getHostAddress());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEvent(PlayerLoginEvent event) {
        UserData data = UserData.getData(event.getPlayer().getUniqueId());

        if(AntiVPN.INSTANCE.vpnHandler.getCached().containsKey(event.getPlayer().getUniqueId())) {
            val result = AntiVPN.INSTANCE.vpnHandler.getCached().get(event.getPlayer().getUniqueId());
            if(result.isProxy() && !event.getPlayer().hasPermission("antivpn.bypass")) {
                event.setKickMessage(StringUtils.formatString(Config.kickMessage, result));
                event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            }
        }
    }

}
