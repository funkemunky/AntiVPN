package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;

import java.util.Collections;
import java.util.List;

public class ClearCacheCommand extends Command {
    @Override
    public String permission() {
        return "antivpn.command.clearcache";
    }

    @Override
    public String name() {
        return "clearcache";
    }

    @Override
    public String[] aliases() {
        return new String[] {"clear", "cc"};
    }

    @Override
    public String description() {
        return "Clear the API response cache if you're having problems.";
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
        AntiVPN.getInstance().getDatabase().clearResponses();
        return "&aCleared all cached API response information!";
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {
        return Collections.emptyList();
    }
}
