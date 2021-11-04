package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;
import dev.brighten.antivpn.utils.MiscUtils;

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
        return "<add/remove> <player/uuid/ip>";
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
            return "&cUsage: /antivpn allowlist " + usage();
        }

        if(args.length == 1)
            return "&cYou have to provide a player to allow or deny exemption.";

        boolean databaseEnabled = AntiVPN.getInstance().getConfig().isDatabaseEnabled();

        if(!databaseEnabled) executor.sendMessage("&cThe database is currently not setup, " +
                "so any changes here will disappear after a restart.");

        if(MiscUtils.isIpv4(args[1])) {
            if(!databaseEnabled) {
                switch(args[0].toLowerCase()) {
                    case "add": {
                        AntiVPN.getInstance().getExecutor().getWhitelistedIps().add(args[1]);
                        return String.format("&aAdded &6%s &ato the exemption allowlist.", args[1]);
                    }
                    case "remove":
                    case "delete": {
                        AntiVPN.getInstance().getExecutor().getWhitelistedIps().remove(args[1]);
                        return String.format("&cRemoved &6%s &cfrom the exemption allowlist.", args[1]);
                    }
                    default: {
                        return "&c\"" + args[0] + "\" is not a valid argument";
                    }
                }
            } else {
                switch(args[0].toLowerCase()) {
                    case "add": {
                        AntiVPN.getInstance().getDatabase().setWhitelisted(args[1], true);
                        return String.format("&aAdded &6%s &a to the exemption allowlist.", args[1]);
                    }
                    case "remove":
                    case "delete": {
                        AntiVPN.getInstance().getDatabase().setWhitelisted(args[1], false);
                        return String.format("&cRemoved &6%s &c from the exemption allowlist.", args[1]);
                    }
                    default: {
                        return "&c\"" + args[0] + "\" is not a valid argument";
                    }
                }
            }
        } else {
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
                switch(args[0].toLowerCase()) {
                    case "add": {
                        AntiVPN.getInstance().getExecutor().getWhitelisted().add(uuid);
                        return String.format("&aAdded &6%s &auuid to the exemption allowlist.", uuid.toString());
                    }
                    case "remove":
                    case "delete": {
                        AntiVPN.getInstance().getExecutor().getWhitelisted().remove(uuid);
                        return String.format("&cRemoved &6%s &cuuid from the exemption allowlist.", uuid.toString());
                    }
                    default: {
                        return "&c\"" + args[0] + "\" is not a valid argument";
                    }
                }
            } else {
                switch(args[0].toLowerCase()) {
                    case "add": {
                        AntiVPN.getInstance().getDatabase().setWhitelisted(uuid, true);
                        return String.format("&aAdded &6%s &auuid to the exemption allowlist.", uuid.toString());
                    }
                    case "remove":
                    case "delete": {
                        AntiVPN.getInstance().getDatabase().setWhitelisted(uuid, false);
                        return String.format("&cRemoved &6%s &cuuid from the exemption allowlist.", uuid.toString());
                    }
                    default: {
                        return "&c\"" + args[0] + "\" is not a valid argument";
                    }
                }
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {
        switch(args.length) {
            case 1: {
                return Arrays.stream(secondArgs)
                        .filter(narg -> narg.toLowerCase().startsWith(args[0].toLowerCase()))
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
