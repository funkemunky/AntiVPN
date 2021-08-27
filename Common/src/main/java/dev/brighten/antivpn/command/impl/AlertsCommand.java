package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;

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
        if(!pgetter.isPresent()) return "&cYou must be a player to execute this command!";

        APIPlayer player = pgetter.get();

        if(player.isAlertsEnabled()) {
            player.setAlertsEnabled(false);
            return "&7You have set your AntiVPN alerts to: &coff";
        } else {
            player.setAlertsEnabled(true);
            return "&7You have set your AntiVPN alerts to: &aon";
        }
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {
        return Collections.emptyList();
    }
}
