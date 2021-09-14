package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;
import dev.brighten.antivpn.message.VpnString;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AlertsCommand extends Command {
    @Override
    public String permission() {
        return "antivpn.command.alerts";
    }

    @Override
    public String name() {
        return "alerts";
    }

    @Override
    public String[] aliases() {
        return new String[] {"valerts", "vpnalerts"};
    }

    @Override
    public String description() {
        return "toggle VPN use alerts";
    }

    @Override
    public String usage() {
        return "";
    }

    @Override
    public String parent() {
        return "antivpn";
    }

    @Override
    public Command[] children() {
        return new Command[0];
    }

    @Override
    public String execute(CommandExecutor executor, String[] args) {
        Optional<APIPlayer> pgetter = executor.getPlayer();
        if(!pgetter.isPresent()) return AntiVPN.getInstance().getMessageHandler()
                .getString("command-misc-playerRequired").getMessage();

        APIPlayer player = pgetter.get();

        player.setAlertsEnabled(!player.isAlertsEnabled());
        player.updateAlertsState();

        return AntiVPN.getInstance().getMessageHandler().getString("command-alerts-toggled")
                .getFormattedMessage(new VpnString.Var<>("state", player.isAlertsEnabled()));
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {
        return Collections.emptyList();
    }
}
