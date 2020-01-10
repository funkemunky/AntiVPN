package dev.brighten.pl.handlers;

import cc.funkemunky.api.utils.JsonMessage;
import dev.brighten.pl.utils.Config;
import dev.brighten.pl.utils.StringUtils;
import dev.brighten.pl.vpn.VPNResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.WeakCollection;
import org.bukkit.entity.Player;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.*;

public class AlertsHandler {
    private Set<Player> hasAlerts = new HashSet<>();

    public void sendAlert(UUID uuid, VPNResponse response) {
        JsonMessage message = new JsonMessage();
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        message.addText(StringUtils.formatString(Config.alertMessage, response)
                .replace("%player%", player.getName()))
                .addHoverText(Config.alertHoverMessage.stream()
                        .map(string -> StringUtils.formatString(string, response)
                                .replace("%player%", player.getName())).toArray(String[]::new));
        hasAlerts.parallelStream().filter(Objects::nonNull)
                .forEach(message::sendToPlayer);
    }

    //TODO When updated Atlas releases, add this functionality.
    public void sendBungeeAlert(UUID uuid, VPNResponse response) {
        //Empty for now.
    }

    public boolean toggleAlerts(Player player) {
        boolean contains;

        if(contains = hasAlerts.contains(player)) hasAlerts.remove(player);
        else hasAlerts.add(player);

        return !contains;
    }
}
