package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
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
    public void onListener(final PlayerLoginEvent event) {
        //If they're exempt, don't check.
        if(AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getUniqueId())) return;
        checkIp(event.getAddress().getHostAddress(), AntiVPN.getInstance().getConfig().cachedResults(), result -> {
            if(result.isSuccess() && result.isProxy()) {
                new BukkitRunnable() {
                    public void run() {
                        Player player = event.getPlayer();

                        if(!player.hasPermission("antivpn.bypass") //Has bypass permission
                                //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                                || AntiVPN.getInstance().getConfig().getPrefixWhitelists().stream()
                                .anyMatch(prefix -> player.getName().startsWith(prefix))) {
                            if (AntiVPN.getInstance().getConfig().kickPlayersOnDetect())
                                player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                                        AntiVPN.getInstance().getConfig().getKickString()));

                            //Ensuring the user wishes to alert to staff
                            if(AntiVPN.getInstance().getConfig().alertToStaff())
                                AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                                        .filter(APIPlayer::isAlertsEnabled)
                                        .forEach(pl -> pl.sendMessage(AntiVPN.getInstance().getConfig()
                                                .alertMessage().replace("%player%", event.getPlayer().getName())
                                                .replace("%reason%", result.getMethod())
                                                .replace("%country%", result.getCountryName())
                                                .replace("%city%", result.getCity())));

                            //In case the user wants to run their own commands instead of using the built in kicking
                            if(AntiVPN.getInstance().getConfig().runCommands())
                                for (String command : AntiVPN.getInstance().getConfig().commands()) {
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                            ChatColor.translateAlternateColorCodes('&',
                                                    command.replace("%player%", event.getPlayer().getName())));
                                }
                        }
                        Bukkit.getLogger().info(player.getPlayer().getName()
                                + " joined on a VPN/Proxy (" + result.getMethod() + ")");
                    }
                }.runTask(BukkitPlugin.pluginInstance);
            } else if(!result.isSuccess()) {
                Bukkit.getLogger().log(Level.WARNING,
                        "The API query was not a success! " +
                                "You may need to upgrade your license on https://funkemunky.cc/shop");
            }
        });
    }
}
