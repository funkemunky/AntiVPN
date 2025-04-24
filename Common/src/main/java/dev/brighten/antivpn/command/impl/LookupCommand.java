package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;
import dev.brighten.antivpn.utils.StringUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LookupCommand extends Command {
    @Override
    public String permission() {
        return "antivpn.command.lookup";
    }

    @Override
    public String name() {
        return "lookup";
    }

    @Override
    public String[] aliases() {
        return new String[] {"check"};
    }

    @Override
    public String description() {
        return "Lookup a player's ip info";
    }

    @Override
    public String usage() {
        return "<player>";
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
        if(args.length == 0) {
            return "&cPlease supply a player to check.";
        }

        Optional<APIPlayer> player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(args[0]);

        if(player.isEmpty()) {
            return String.format("&cNo player found with the name \"%s\"", args[0]);
        }

        AntiVPN.getInstance().getExecutor()
                .checkIp(player.get().getIp().getHostAddress())
                .thenAccept(result -> {
                    if(!result.isSuccess()) {
                        executor.sendMessage("&cThere was an error trying to find the " +
                                "information of this player.");
                        return;
                    }

                    executor.sendMessage(StringUtil.line("&8"));
                    executor.sendMessage("&6&l" + player.get().getName() + "&7&l's Connection Information");
                    executor.sendMessage("");
                    executor.sendMessage("&e%s&8: &f%s", "Proxy", result.isProxy()
                            ? "&a" + result.getMethod() : "&cNo");
                    executor.sendMessage("&e%s&8: &f%s", "ISP", result.getIsp());
                    executor.sendMessage("&e%s&8: &f%s", "Country", result.getCountryName());
                    executor.sendMessage("&e%s&8: &f%s", "City", result.getCity());
                    executor.sendMessage("&e%s&8: &f%s", "Coordinates", result.getLatitude()
                            + "&7/&f" + result.getLongitude());
                    executor.sendMessage(StringUtil.line("&8"));
                });


        return "&7Looking up the IP information for player " + player.get().getName() + "...";
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {

        if(args.length == 1) return AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                .map(APIPlayer::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());

        return Collections.emptyList();
    }
}
