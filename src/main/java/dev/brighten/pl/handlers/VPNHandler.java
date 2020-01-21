package dev.brighten.pl.handlers;

import cc.funkemunky.api.Atlas;
import cc.funkemunky.api.utils.MiscUtils;
import cc.funkemunky.api.utils.RunUtils;
import cc.funkemunky.api.utils.Tuple;
import dev.brighten.pl.AntiVPN;
import dev.brighten.pl.data.UserData;
import dev.brighten.pl.listeners.impl.VPNCheckEvent;
import dev.brighten.pl.utils.Config;
import dev.brighten.pl.utils.StringUtils;
import dev.brighten.pl.vpn.VPNResponse;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class VPNHandler {
    public final Deque<Tuple<UUID, String>> queue = new LinkedBlockingDeque<>();
    public final AtomicBoolean checking = new AtomicBoolean(true);
    @Getter
    private Map<UUID, VPNResponse> cached = new HashMap<>();
    public ExecutorService thread = Executors.newSingleThreadScheduledExecutor();

    public void run() {
        thread.execute(() -> {
            if(checking.get()) {
                Tuple<UUID, String> value;

                while((value = queue.poll()) != null) {
                    val response = AntiVPN.INSTANCE.vpnAPI.getResponse(value.two);

                    UserData data = UserData.getData(value.one);

                    if(data != null && data.getPlayer() != null) {
                        data.response = response;

                        alert(response, value.one);
                        kick(response, value.one);
                    } else cached.put(value.one, response);
                }
            } else thread.shutdown();
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

            if(player != null) {
                RunUtils.task(() -> {
                    String message = StringUtils.formatString(Config.kickMessage, response);
                    player.kickPlayer(message);
                });
            }
        }
    }

    public void checkPlayer(Player player) {
        checkPlayer(player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
    }

    public void checkPlayer(UUID uuid, String address) {
        queue.add(new Tuple<>(uuid, address));
    }
}
