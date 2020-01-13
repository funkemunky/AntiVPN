package dev.brighten.pl.handlers;

import cc.funkemunky.api.utils.JsonMessage;
import dev.brighten.pl.data.UserData;
import dev.brighten.pl.utils.Config;
import dev.brighten.pl.utils.StringUtils;
import dev.brighten.pl.vpn.VPNResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class AlertsHandler {

    public void sendAlert(UUID uuid, VPNResponse response) {
        JsonMessage message = new JsonMessage();
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        message.addText(StringUtils.formatString(Config.alertMessage, response)
                .replace("%player%", player.getName()))
                .addHoverText(Config.alertHoverMessage.stream()
                        .map(string -> StringUtils.formatString(string, response)
                                .replace("%player%", player.getName())).toArray(String[]::new));
        UserData.dataMap.values().parallelStream().filter(data -> data.hasAlerts)
                .forEach(data -> message.sendToPlayer(data.getPlayer()));
    }

    //TODO When updated Atlas releases, add this functionality.
    public void sendBungeeAlert(UUID uuid, VPNResponse response) {
        //Empty for now.
    }

    public boolean toggleAlerts(Player player) {
        UserData data = UserData.getData(player.getUniqueId());

        return data.hasAlerts = !data.hasAlerts;
    }
}
