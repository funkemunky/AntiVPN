package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;
import dev.brighten.antivpn.utils.CIDRUtils;
import dev.brighten.antivpn.utils.MiscUtils;

import java.net.UnknownHostException;
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

        boolean databaseEnabled = AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled();

        if(!databaseEnabled) executor.sendMessage("&cThe database is currently not setup, " +
                "so any changes here will disappear after a restart.");

        CIDRUtils cidrUtils;

        try {
            cidrUtils = new CIDRUtils(args[1]);
        } catch(IllegalArgumentException | UnknownHostException e) {
            cidrUtils = null;
        }

        if(cidrUtils != null) {
            if(!databaseEnabled) {
                return switch (args[0].toLowerCase()) {
                    case "add", "insert" -> {
                        AntiVPN.getInstance().getExecutor().getWhitelistedIps().add(cidrUtils.getCidr());
                        yield String.format("&aAdded &6%s &ato exemption allowlist.", cidrUtils.getCidr());
                    }
                    case "remove", "delete" -> {
                        AntiVPN.getInstance().getExecutor().getWhitelistedIps().remove(cidrUtils.getCidr());
                        yield String.format("&cRemoved &%s &cfrom the exemption allowlist.", cidrUtils.getCidr());
                    }
                    default -> "&c\"" + args[0] + "\" is not a valid argument";
                };
            } else return switch (args[0].toLowerCase()) {
                case "add", "insert" -> {
                    AntiVPN.getInstance().getExecutor().getWhitelistedIps().add(cidrUtils.getCidr());
                    AntiVPN.getInstance().getDatabase().addWhitelist(cidrUtils.getCidr());
                    yield String.format("&aAdded &6%s &ato exemption allowlist.", cidrUtils.getCidr());
                }
                case "remove", "delete" -> {
                    AntiVPN.getInstance().getExecutor().getWhitelistedIps().remove(cidrUtils.getCidr());
                    AntiVPN.getInstance().getDatabase().removeWhitelist(cidrUtils.getCidr());
                    yield String.format("&cRemoved &6%s &cfrom the exemption allowlist.", cidrUtils.getCidr());
                }
                default -> "&c\"" + args[0] + "\" is not a valid argument";
            };
        }
        if(MiscUtils.isIpv4(args[1])) {
            if(!databaseEnabled) {
                return switch(args[0].toLowerCase()) {
                    case "add", "insert" -> {
                        AntiVPN.getInstance().getExecutor().getWhitelistedIps().add(args[1] + "/32");
                        AntiVPN.getInstance().getDatabase().addWhitelist(args[1] + "/32");
                        yield String.format("&aAdded &6%s &ato the exemption allowlist.", args[1] + "/32");
                    }
                    case "remove", "delete" -> {
                        AntiVPN.getInstance().getExecutor().getWhitelistedIps().remove(args[1] + "/32");
                        AntiVPN.getInstance().getDatabase().removeWhitelist(args[1] + "/32");
                        yield String.format("&cRemoved &6%s &cfrom the exemption allowlist.", args[1] + "/32");
                    }
                    default -> "&c\"" + args[0] + "\" is not a valid argument";
                };
            } else return switch (args[0].toLowerCase()) {
                    case "add", "insert" -> {
                        AntiVPN.getInstance().getDatabase().addWhitelist(args[1] + "/32");
                        yield String.format("&aAdded &6%s &a to the exemption allowlist.", args[1] + "/32");
                    }
                    case "remove", "delete" -> {
                        AntiVPN.getInstance().getDatabase().removeWhitelist(args[1] + "/32");
                        yield String.format("&cRemoved &6%s &c from the exemption allowlist.", args[1] + "/32");
                    }
                    default -> "&c\"" + args[0] + "\" is not a valid argument";
            };
        } else {
            UUID uuid;
            try {
                uuid = UUID.fromString(args[1]);
            } catch(IllegalArgumentException e) {
                Optional<APIPlayer> player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(args[1]);

                if(player.isEmpty()) {
                    return "&cThe player \"" + args[1] + "\" is not online, so please provide a UUID.";
                }

                uuid = player.get().getUuid();
            }

            if(!databaseEnabled) {
                return switch (args[0].toLowerCase()) {
                    case "add" -> {
                        AntiVPN.getInstance().getExecutor().getWhitelisted().add(uuid);
                        yield String.format("&aAdded &6%s &auuid to the exemption allowlist.", uuid.toString());
                    }
                    case "remove", "delete" -> {
                        AntiVPN.getInstance().getExecutor().getWhitelisted().remove(uuid);
                        yield String.format("&cRemoved &6%s &cuuid from the exemption allowlist.", uuid.toString());
                    }
                    default -> "&c\"" + args[0] + "\" is not a valid argument";
                };
            } else {
                return switch (args[0].toLowerCase()) {
                    case "add" -> {
                        AntiVPN.getInstance().getDatabase().addWhitelist(uuid);
                        yield String.format("&aAdded &6%s &auuid to the exemption allowlist.", uuid.toString());
                    }
                    case "remove", "delete" -> {
                        AntiVPN.getInstance().getDatabase().removeWhitelist(uuid);
                        yield String.format("&cRemoved &6%s &cuuid from the exemption allowlist.", uuid.toString());
                    }
                    default -> "&c\"" + args[0] + "\" is not a valid argument";
                };
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {
        return switch (args.length) {
            case 1 -> Arrays.stream(secondArgs)
                    .filter(narg -> narg.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            case 2 -> AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                    .map(APIPlayer::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            default -> Collections.emptyList();
        };
    }
}
