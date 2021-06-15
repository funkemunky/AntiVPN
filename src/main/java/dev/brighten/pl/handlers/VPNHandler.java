package dev.brighten.pl.handlers;

import cc.funkemunky.api.bungee.BungeeAPI;
import cc.funkemunky.api.utils.RunUtils;
import dev.brighten.pl.AntiVPN;
import dev.brighten.pl.config.Config;
import dev.brighten.pl.data.UserData;
import dev.brighten.pl.listeners.impl.VPNCheckEvent;
import dev.brighten.pl.utils.StringUtils;
import dev.brighten.pl.vpn.VPNResponse;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class VPNHandler {
    @Getter
    private Map<UUID, VPNResponse> cached = new HashMap<>();
    public ExecutorService thread = Executors.newSingleThreadExecutor();

    public void run() {
        RunUtils.taskTimerAsync(() -> cached.clear(), 24000, 24000); //Clear cache every 20 minutes
    }
    private void alert(VPNResponse response, UUID uuid) {
        if(Config.alertBungee) {
            AntiVPN.INSTANCE.alertsHandler.sendBungeeAlert(uuid, response); //Empty method until Atlas v1.7 releases.
        } else {
            AntiVPN.INSTANCE.alertsHandler.sendAlert(uuid, response);
        }
    }

    private void kick(VPNResponse response, UUID uuid) {
        RunUtils.task(() -> { //Putting on to main thread otherwise we risk errors.
            if(Config.kickBungee) {
                BungeeAPI.kickPlayer(uuid, StringUtils.formatString(Config.kickMessage, response));
            } else {
                Player player = Bukkit.getPlayer(uuid);

                if(player != null) {
                    String message = StringUtils.formatString(Config.kickMessage, response);
                    player.kickPlayer(message);
                }
            }
        });
    }

    public void shutdown() {
        thread.shutdown();
    }

    public void checkPlayer(Player player) {
        checkPlayer(player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
    }

    public void checkPlayer(UUID uuid, String address) {
        thread.execute(() -> {
            val response = AntiVPN.INSTANCE.vpnAPI.getResponse(address);

            if(response != null) {
                UserData data = UserData.getData(uuid);

                VPNCheckEvent vpnCheckEvent = new VPNCheckEvent(uuid, response);

                Bukkit.getPluginManager().callEvent(vpnCheckEvent); //Calling VPN check event for other plugins to use.

                if(data != null && data.getPlayer() != null) {
                    data.response = response;

                    if(data.response.isProxy() && !data.getPlayer().hasPermission("antivpn.bypass")) {
                        alert(response, uuid);
                        kick(response, uuid);
                    }
                } else cached.put(uuid, response);
            }
        });
    }
}
