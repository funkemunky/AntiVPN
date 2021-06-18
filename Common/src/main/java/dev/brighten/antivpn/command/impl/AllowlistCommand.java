package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;

import java.util.*;
import java.util.stream.Collectors;

public class AllowlistCommand extends Command {

    private static final String[] secondArgs = new String[] {"add", "remove"};

    @Override
    public String permission() {
        return "antivpn.command.allowlist";
    }

    @Override
    public String name() {
        return "allowlist";
    }

    @Override
    public String[] aliases() {
        return new String[] {"whitelist"};
    }

    @Override
    public String description() {
        return "Add/remove players to/from exemption list.";
    }

    @Override
    public String usage() {
        return "<add/remove> <player/uuid>";
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
        if(args.length == 0 || Arrays.stream(secondArgs).noneMatch(arg -> arg.equalsIgnoreCase(args[0]))) {
            return "&cUsage: /antivpn allowlist <add/remove> <player>";
        }

        if(args.length == 1)
            return "&cYou have to provide a player to allow or deny exemption.";

        boolean databaseEnabled = AntiVPN.getInstance().getConfig().isDatabaseEnabled();

        if(!databaseEnabled) executor.sendMessage("&cThe database is currently not setup, " +
                "so any changes here will disappear after a restart.");

        UUID uuid = null;
        try {
            uuid = UUID.fromString(args[1]);
        } catch(IllegalArgumentException e) {
            Optional<APIPlayer> player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(args[1]);

            if(!player.isPresent()) {
                return "&cThe player \"" + args[1] + "\" is not online, so please provide a UUID.";
            }

            uuid = player.get().getUuid();
        }

        if(!databaseEnabled) {
            if(args[0].equalsIgnoreCase("add")) {
                AntiVPN.getInstance().getExecutor().getWhitelisted().add(uuid);
                return String.format("&aAdded &6%s &auuid to the exemption allowlist.", uuid.toString());
            } else {
                AntiVPN.getInstance().getExecutor().getWhitelisted().remove(uuid);
                return String.format("&cRemoved &6%s &cuuid from the exemption allowlist.", uuid.toString());
            }
        } else {
            if(args[0].equalsIgnoreCase("add")) {
                AntiVPN.getInstance().getDatabase().setWhitelisted(uuid, true);
                return String.format("&aAdded &6%s &auuid to the exemption allowlist.", uuid.toString());
            } else {
                AntiVPN.getInstance().getDatabase().setWhitelisted(uuid, false);
                return String.format("&cRemoved &6%s &cuuid from the exemption allowlist.", uuid.toString());
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {
        switch(args.length) {
            case 1: {
                return Arrays.stream(args)
                        .filter(narg -> narg.startsWith(args[0]))
                        .collect(Collectors.toList());
            }
            case 2: {
                return AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                        .map(APIPlayer::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }
}
