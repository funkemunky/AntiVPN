package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.VPNExecutor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class BungeeListener extends VPNExecutor implements Listener {

    private ScheduledTask cacheResetTask;

    @Override
    public void registerListeners() {
        BungeePlugin.pluginInstance.getProxy().getPluginManager()
                .registerListener(BungeePlugin.pluginInstance, this);
    }

    @Override
    public void runCacheReset() {
        cacheResetTask = BungeePlugin.pluginInstance.getProxy().getScheduler().schedule(BungeePlugin.pluginInstance,
                this::resetCache, 20, 20, TimeUnit.MINUTES);
    }

    @Override
    public void shutdown() {
        if(cacheResetTask != null) {
            cacheResetTask.cancel();
            cacheResetTask = null;
        }
        threadExecutor.shutdown();
        BungeePlugin.pluginInstance.getProxy().getPluginManager().unregisterListener(this);
    }

    @EventHandler
    public void onListener(final PostLoginEvent event) {
        if(event.getPlayer().hasPermission("antivpn.bypass") //Has bypass permission
                || AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getUniqueId()) //Is exempt
                //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                || AntiVPN.getInstance().getConfig().getPrefixWhitelists().stream()
                .anyMatch(prefix -> event.getPlayer().getName().startsWith(prefix))) return;

        checkIp(event.getPlayer().getAddress().getAddress().getHostAddress(),
                AntiVPN.getInstance().getConfig().cachedResults(), result -> {
            if(result.isSuccess() && result.isProxy()) {
                if(AntiVPN.getInstance().getConfig().kickPlayersOnDetect())
                event.getPlayer().disconnect(TextComponent.fromLegacyText(ChatColor
                        .translateAlternateColorCodes('&',
                                AntiVPN.getInstance().getConfig().getKickString())));
                BungeeCord.getInstance().getLogger().info(event.getPlayer().getName()
                        + " joined on a VPN/Proxy (" + result.getMethod() + ")");

                if(AntiVPN.getInstance().getConfig().runCommands()) {
                    for (String command : AntiVPN.getInstance().getConfig().commands()) {
                        BungeeCord.getInstance().getPluginManager()
                                .dispatchCommand(BungeeCord.getInstance().getConsole(),
                                        ChatColor.translateAlternateColorCodes('&',
                                                command.replace("%player%", event.getPlayer().getName())));
                    }
                }
            } else if(!result.isSuccess()) {
                BungeeCord.getInstance().getLogger()
                        .log(Level.WARNING,
                                "The API query was not a success! " +
                                        "You may need to upgrade your license on https://funkemunky.cc/shop");
            }
        });
    }
}
