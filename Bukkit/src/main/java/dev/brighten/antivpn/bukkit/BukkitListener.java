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
        checkIp(event.getAddress().getHostAddress(), AntiVPN.getInstance().getConfig().cachedResults(), result -> {
            if(result.isSuccess() && result.isProxy()) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(ChatColor.translateAlternateColorCodes('&',
                        AntiVPN.getInstance().getConfig().getKickString()));
                Optional.ofNullable(Bukkit.getPlayer(event.getUniqueId())).ifPresent(player -> {
                    new BukkitRunnable() {
                        public void run() {
                            player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                                    AntiVPN.getInstance().getConfig().getKickString()));
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
