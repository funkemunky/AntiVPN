package dev.brighten.pl.commands;

import cc.funkemunky.api.commands.ancmd.Command;
import cc.funkemunky.api.commands.ancmd.CommandAdapter;
import cc.funkemunky.api.utils.Color;
import cc.funkemunky.api.utils.Init;
import cc.funkemunky.api.utils.MathUtils;
import dev.brighten.pl.AntiVPN;

@Init(commands = true)
public class ReloadCommand {

    @Command(name = "kaurivpn.reload", description = "reload KauriVPN.", display = "reload",
            aliases = {"antivpn.reload"}, permission = "kvpn.command.reload")
    public void onCommand(CommandAdapter cmd) {
        long start = System.nanoTime();
        task(cmd, "Reloading config");
        AntiVPN.INSTANCE.reloadConfig();

        task(cmd, "Unloading KauriVPN");
        AntiVPN.INSTANCE.disable();

        task(cmd, "Loading KauriVPN");
        AntiVPN.INSTANCE.enable();

        double complete = System.nanoTime() - start / 1E6D;

        cmd.getSender().sendMessage(Color.Green + "Reload completed in "
                + MathUtils.round(complete, 2) + "ms.");
    }

    private static void task(CommandAdapter adapter, String task) {
        adapter.getSender().sendMessage(Color.translate("&7" + task + "..."));
    }
}
