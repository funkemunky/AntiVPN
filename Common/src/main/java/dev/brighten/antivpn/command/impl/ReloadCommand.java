package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;

import java.util.Collections;
import java.util.List;

public class ReloadCommand extends Command {
    @Override
    public String permission() {
        return "antivpn.command.reload";
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public String[] aliases() {
        return new String[0];
    }

    @Override
    public String description() {
        return "Reload the plugin";
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
        // Loading changes from the config.yml
        AntiVPN.getInstance().reloadConfig();

        // Updating the cache of these values in VPNConfig
        AntiVPN.getInstance().getVpnConfig().update();

        AntiVPN.getInstance().getMessageHandler().reloadStrings();

        AntiVPN.getInstance().reloadDatabase();

        return AntiVPN.getInstance().getMessageHandler().getString("command-reload-complete").getMessage();
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {
        return Collections.emptyList();
    }
}
