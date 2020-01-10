package dev.brighten.pl.commands;

import cc.funkemunky.api.commands.ancmd.Command;
import cc.funkemunky.api.commands.ancmd.CommandAdapter;
import cc.funkemunky.api.utils.Init;
import dev.brighten.pl.AntiVPN;

@Init(commands = true)
public class AlertsCommand {

    @Command(name = "kaurivpn.alerts", description = "toggle vpn alerts",
            display = "alerts", playerOnly = true, permission = {"kvpn.alerts", "kvpn.command.alerts"})
    public void onCommand(CommandAdapter cmd) {
        boolean toggled = AntiVPN.INSTANCE.alertsHandler.toggleAlerts(cmd.getPlayer());
        cmd.getSender().sendMessage(toggled
                ? "&aYou are now viewing vpn detection alerts."
                : "&cYou are no longer viewing vpn detection alerts.");
    }
}
