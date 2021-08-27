package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.VPNExecutor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.logging.Level;

public class BukkitListener extends VPNExecutor implements Listener {

    private BukkitTask cacheResetTask;
    @Override
    public void registerListeners() {
        BukkitPlugin.pluginInstance.getServer().getPluginManager()
                .registerEvents(this, BukkitPlugin.pluginInstance);
    }

    @Override
    public void runCacheReset() {
        cacheResetTask = new BukkitRunnable() {
            public void run() {
                resetCache();
            }
        }.runTaskTimerAsynchronously(BukkitPlugin.pluginInstance, 24000, 24000); //Reset cache every 20 minutes

        HandlerList.unregisterAll(this);
        threadExecutor.shutdown();
    }

    @Override
    public void shutdown() {
        if(cacheResetTask != null && !cacheResetTask.isCancelled()) cacheResetTask.cancel();
    }

    @EventHandler
    public void onListener(final AsyncPlayerPreLoginEvent event) {
        //If they're exempt, don't check.
        if(AntiVPN.getInstance().getExecutor().isWhitelisted(event.getUniqueId())) return;
        checkIp(event.getAddress().getHostAddress(), AntiVPN.getInstance().getConfig().cachedResults(), result -> {
            if(result.isSuccess() && result.isProxy()) {
                if(AntiVPN.getInstance().getConfig().kickPlayersOnDetect()) {
                    event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                    event.setKickMessage(ChatColor.translateAlternateColorCodes('&',
                            AntiVPN.getInstance().getConfig().getKickString()));
                }
                Optional.ofNullable(Bukkit.getPlayer(event.getUniqueId())).ifPresent(player -> {
                    new BukkitRunnable() {
                        public void run() {
                            if(!player.hasPermission("antivpn.bypass") //Has bypass permission
                                    //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                                    || AntiVPN.getInstance().getConfig().getPrefixWhitelists().stream()
                                    .anyMatch(prefix -> player.getName().startsWith(prefix))) {
                                if (AntiVPN.getInstance().getConfig().kickPlayersOnDetect())
                                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                                            AntiVPN.getInstance().getConfig().getKickString()));

                                for (String command : AntiVPN.getInstance().getConfig().commands()) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                            ChatColor.translateAlternateColorCodes('&',
                                                    command.replace("%player%", event.getName())));
                                }
                            }
                           Bukkit.getLogger().info(player.getPlayer().getName()
                                    + " joined on a VPN/Proxy (" + result.getMethod() + ")");
                        }
                    }.runTask(BukkitPlugin.pluginInstance);
                });
            } else if(!result.isSuccess()) {
                Bukkit.getLogger().log(Level.WARNING,
                        "The API query was not a success! " +
                                "You may need to upgrade your license on https://funkemunky.cc/shop");
            }
        });
    }
}
