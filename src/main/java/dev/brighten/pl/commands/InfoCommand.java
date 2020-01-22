package dev.brighten.pl.commands;

import cc.funkemunky.api.commands.ancmd.Command;
import cc.funkemunky.api.commands.ancmd.CommandAdapter;
import cc.funkemunky.api.utils.Color;
import cc.funkemunky.api.utils.Init;
import cc.funkemunky.api.utils.MiscUtils;
import dev.brighten.db.utils.json.JSONException;
import dev.brighten.pl.AntiVPN;
import dev.brighten.pl.data.UserData;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Init(commands = true)
public class InfoCommand {

    private static String LINE =  MiscUtils.line(Color.Dark_Gray);

    @Command(name = "kaurivpn.info", description = "view a player's vpn info.", display = "info [player]",
            permission = "kvpn.command.info", aliases = {"antivpn.info"})
    public void onCommand(CommandAdapter cmd) {
        UserData data;
        if(cmd.getArgs().length == 0) {
            if(cmd.getPlayer() != null) data = UserData.getData(cmd.getPlayer().getUniqueId());
            else {
                cmd.getSender().sendMessage(Color.Red + "You cannot view your own info as you're not a player.");
                return;
            }
        } else {
            Player player;
            if((player = Bukkit.getPlayer(cmd.getArgs()[0])) != null) {
                data = UserData.getData(player.getUniqueId());
            } else {
                cmd.getSender().sendMessage(Color.Red + "Could not find that player. Is he/she even online?");
                return;
            }
        }
        sendData(cmd, data);
    }

    private static void sendData(CommandAdapter cmd, UserData data) {
        if(data.response != null) {
            cmd.getSender().sendMessage(LINE);
            sendMsg(cmd, "&6&l" + data.getPlayer().getName() + "'s Information");
            sendMsg(cmd, "");
            try {
                val json = data.response.toJson();
                json.keySet().stream()
                        .filter(key -> {
                            switch(key) {
                                case "ip":
                                case "city":
                                case "success":
                                case "queriesLeft":
                                case "locationString":
                                case "usedAdvanced":
                                    return false;
                                default:
                                    return true;
                            }
                        })
                        .forEach(key -> {
                            try {
                                sendMsg(cmd, "&7" + key.toUpperCase() + "&8: &f" + json.get(key));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (JSONException e) {
                sendMsg(cmd, "&cThere was an error parsing the VPN response.");
                e.printStackTrace();
            }
            cmd.getSender().sendMessage(LINE);
        } else if(AntiVPN.INSTANCE.vpnHandler.getCached().containsKey(data.uuid)) {
            data.response = AntiVPN.INSTANCE.vpnHandler.getCached()
                    .computeIfPresent(data.uuid,
                            (key, value) -> AntiVPN.INSTANCE.vpnHandler.getCached().remove(key));

            cmd.getSender().sendMessage(LINE);
            sendMsg(cmd, "&6&l" + data.getPlayer().getName() + "'s Information");
            sendMsg(cmd, "");
            try {
                val json = data.response.toJson();
                json.keySet().stream()
                        .filter(key -> {
                            switch(key) {
                                case "ip":
                                case "city":
                                case "success":
                                case "queriesLeft":
                                case "locationString":
                                case "usedAdvanced":
                                    return false;
                                default:
                                    return true;
                            }
                        })
                        .forEach(key -> {
                            try {
                                sendMsg(cmd, "&7" + key.toUpperCase() + "&8: &f" + json.get(key));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (JSONException e) {
                sendMsg(cmd, "&cThere was an error parsing the VPN response.");
                e.printStackTrace();
            }
            cmd.getSender().sendMessage(LINE);
        } else cmd.getSender().sendMessage(Color.Red + "This user was not checked for a vpn.");
    }

    private static void sendMsg(CommandAdapter cmd, String msg) {
        cmd.getSender().sendMessage(Color.translate(msg));
    }
}
