package dev.brighten.pl.commands;

import cc.funkemunky.api.Atlas;
import cc.funkemunky.api.commands.ancmd.Command;
import cc.funkemunky.api.commands.ancmd.CommandAdapter;
import cc.funkemunky.api.utils.Init;

@Init(commands = true)
public class VpnCommand {

    @Command(name = "kaurivpn", description = "The Kauri AntiVPN main command.",
            aliases = {"antivpn", "avpn", "kvpn"}, permission = "kvpn.command")
    public void onCommand(CommandAdapter cmd) {
        Atlas.getInstance().getCommandManager().runHelpMessage(cmd,
                cmd.getSender(), Atlas.getInstance().getCommandManager().getDefaultScheme());
    }
}
