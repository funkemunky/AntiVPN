package dev.brighten.pl.handlers;

import cc.funkemunky.api.Atlas;
import cc.funkemunky.api.utils.RunUtils;
import cc.funkemunky.api.utils.Tuple;
import dev.brighten.pl.AntiVPN;
import dev.brighten.pl.listeners.impl.VPNCheckEvent;
import dev.brighten.pl.utils.Config;
import dev.brighten.pl.utils.StringUtils;
import dev.brighten.pl.vpn.VPNResponse;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VPNHandler {
    private Queue<Tuple<UUID, String>> queue = new ConcurrentLinkedQueue<>();

    public void run() {

    }

    private void runCheck() {
        AntiVPN.INSTANCE.vpnAPI.vpnThread.execute(() -> {
            Tuple<UUID, String> element;
            while((element = queue.poll()) != null) {
                val response = AntiVPN.INSTANCE.vpnAPI.getResponse(element.two);
                if(response != null && response.isSuccess()) {
                    VPNCheckEvent event = new VPNCheckEvent(response);
                    if(Config.fireEvent)
                        RunUtils.task(() -> Bukkit.getPluginManager().callEvent(event), AntiVPN.INSTANCE);

                    if(response.isProxy()) {
                        if(Config.alertStaff) alert(response, element.one);
                        if(Config.kickPlayers) kick(response, element.one);
                    }
                }
            }
            runCheck();
        });
    }

    private void alert(VPNResponse response, UUID uuid) {
        if(Config.alertBungee) {
            AntiVPN.INSTANCE.alertsHandler.sendBungeeAlert(uuid, response); //Empty method until Atlas v1.7 releases.
        } else {
            AntiVPN.INSTANCE.alertsHandler.sendAlert(uuid, response);
        }
    }

    private void kick(VPNResponse response, UUID uuid) {
        if(Config.kickBungee) {
            Atlas.getInstance().getBungeeManager().getBungeeAPI()
                    .kickPlayer(uuid, StringUtils.formatString(Config.kickMessage, response));
        } else {
            Player player = Bukkit.getPlayer(uuid);

            if(player != null)
                player.kickPlayer(StringUtils.formatString(Config.kickMessage, response));
        }
    }

    public void checkPlayer(Player player) {
        queue.add(new Tuple<>(player.getUniqueId(), player.getAddress().getAddress().getHostAddress()));
    }
}
