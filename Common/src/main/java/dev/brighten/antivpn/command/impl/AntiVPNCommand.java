package dev.brighten.antivpn.command.impl;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;
import dev.brighten.antivpn.utils.StringUtil;

import java.util.*;
import java.util.stream.Collectors;

public class AntiVPNCommand extends Command {
    @Override
    public String permission() {
        return "antivpn.command";
    }

    @Override
    public String name() {
        return "antivpn";
    }

    @Override
    public String[] aliases() {
        return new String[] {"kaurivpn", "kvpn", "vpn", "avpn"};
    }

    @Override
    public String description() {
        return "The main help command";
    }

    @Override
    public String usage() {
        return "";
    }

    @Override
    public String parent() {
        return "";
    }

    @Override
    public Command[] children() {
        return new Command[] {new LookupCommand(), new AllowlistCommand(), new AlertsCommand()};
    }

    @Override
    public String execute(CommandExecutor uuid, String[] args) {
        List<String> messages = new ArrayList<>();

        messages.add(StringUtil.line("&8"));
        messages.add("&6&lAntiVPN Help Page");
        messages.add("");
        for (Command cmd : AntiVPN.getInstance().getCommands()) {
            messages.add(String.format("&8/&f%s &8- &7&o%s", "&7" + cmd.parent()
                    + (cmd.parent().length() > 0 ? " " : "") + "&f" + cmd.name() + " &7"
                            + cmd.usage(), description()));
        }
        for (Command child : children()) {
            messages.add(String.format("&8/&f%s &8- &7&o%s", "&7" + child.parent()
                    + (child.parent().length() > 0 ? " " : "") + "&f" + child.name() + " &7"
                    + child.usage(), description()));
        }

        messages.add(StringUtil.line("&8"));

        return String.join("\n", messages);
    }

    @Override
    public List<String> tabComplete(CommandExecutor executor, String alias, String[] args) {
        if(args.length == 1)
        return Arrays.stream(children())
                .map(Command::name)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());

        return Collections.emptyList();
    }
}
