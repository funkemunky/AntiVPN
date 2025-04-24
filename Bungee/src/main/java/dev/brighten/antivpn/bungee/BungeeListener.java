package dev.brighten.antivpn.bungee;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.web.objects.VPNResponse;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class BungeeListener extends VPNExecutor implements Listener {

    private ScheduledTask cacheResetTask;

    private final Cache<UUID, VPNResponse> responseCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(2000)
            .build();

    @Override
    public void registerListeners() {
        BungeePlugin.pluginInstance.getProxy().getPluginManager()
                .registerListener(BungeePlugin.pluginInstance, this);
    }

    @Override
    public void log(Level level, String log, Object... objects) {
        BungeePlugin.pluginInstance.getProxy().getLogger().log(Level.INFO, String.format(log, objects));
    }

    @Override
    public void log(String log, Object... objects) {
        log(Level.INFO, String.format(log, objects));
    }

    @Override
    public void logException(String message, Throwable ex) {
        BungeePlugin.pluginInstance.getProxy().getLogger().log(Level.SEVERE, message, ex);
    }

    @Override
    public void disablePlugin() {
        BungeePlugin.pluginInstance.getProxy().getPluginManager().unregisterListeners(BungeePlugin.pluginInstance);
        if(cacheResetTask != null) {
            cacheResetTask.cancel();
            cacheResetTask = null;
        }
        BungeePlugin.pluginInstance.getProxy().getPluginManager().unregisterCommands(BungeePlugin.pluginInstance);
        BungeePlugin.pluginInstance.onDisable();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onListener(final PreLoginEvent event) {
        if(!responseCache.asMap().containsKey(event.getConnection().getUniqueId())) return;

        VPNResponse cached = responseCache.getIfPresent(event.getConnection().getUniqueId());

        if(cached != null && cached.isProxy()) {
            event.setCancelled(true);
            event.setReason(TextComponent.fromLegacy(ChatColor
                    .translateAlternateColorCodes('&',
                            AntiVPN.getInstance().getVpnConfig().getKickString())));
            AntiVPN.getInstance().getExecutor().log(Level.INFO,
                    "%s was kicked from pre-login proxy cache.",
                    event.getConnection().getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onListener(final PostLoginEvent event) {
        if(event.getPlayer().hasPermission("antivpn.bypass") //Has bypass permission
                || AntiVPN.getInstance().getVpnConfig().getPrefixWhitelists().stream()
                .anyMatch(prefix -> event.getPlayer().getName().startsWith(prefix))) return;

        String address = event.getPlayer().getSocketAddress().toString();

        if(AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getUniqueId())) {
            AntiVPN.getInstance().getExecutor().log("UUID is whitelisted: %s",
                    event.getPlayer().getUniqueId().toString());
            return;
        }

        //If the IP is whitelisted, we don't want to kick them
        if(AntiVPN.getInstance().getExecutor().isWhitelisted(address)) {
            AntiVPN.getInstance().getExecutor().log("IP is whitelisted: %s", address);
            return;
        }

        checkIp(address)
                .thenAccept(result -> {
                    if(result.isSuccess()) {
                        //If the player is whitelisted, we don't want to kick them
                        responseCache.put(event.getPlayer().getUniqueId(), result);
                        if(!AntiVPN.getInstance().getVpnConfig().countryList().isEmpty()
                                // This bit of code will decide whether or not to kick the player
                                // If it contains the code and it is set to whitelist, it will not kick as they are equal
                                // and vise versa. However, if the contains does not match the state, it will kick.
                                && AntiVPN.getInstance().getVpnConfig().countryList()
                                .contains(result.getCountryCode()) != AntiVPN.getInstance().getVpnConfig().whitelistCountries()) {
                            //Using our built in kicking system if no commands are configured
                            if(AntiVPN.getInstance().getVpnConfig().countryKickCommands().isEmpty()) {
                                final String kickReason = AntiVPN.getInstance().getVpnConfig()
                                        .countryVanillaKickReason();
                                // Kicking our player
                                event.getPlayer().disconnect(TextComponent.fromLegacy(ChatColor
                                        .translateAlternateColorCodes('&',
                                                kickReason
                                                        .replace("%player%", event.getPlayer().getName())
                                                        .replace("%country%", result.getCountryName())
                                                        .replace("%code%", result.getCountryCode()))));
                            } else {
                                for (String cmd : AntiVPN.getInstance().getVpnConfig().countryKickCommands()) {
                                    final String formattedCommand = ChatColor.translateAlternateColorCodes('&',
                                            cmd.replace("%player%", event.getPlayer().getName())
                                                    .replace("%country%", result.getCountryName())
                                                    .replace("%code%", result.getCountryCode()));

                                    // Runs our command from console
                                    BungeePlugin.pluginInstance.getProxy().getPluginManager().dispatchCommand(
                                            BungeePlugin.pluginInstance.getProxy().getConsole(), formattedCommand);
                                }
                            }
                        } else if(result.isProxy()) {
                            if(AntiVPN.getInstance().getVpnConfig().kickPlayersOnDetect())
                                event.getPlayer().disconnect(TextComponent.fromLegacy(ChatColor
                                        .translateAlternateColorCodes('&',
                                                AntiVPN.getInstance().getVpnConfig().getKickString())));
                            BungeePlugin.pluginInstance.getProxy().getLogger().info(event.getPlayer().getName()
                                    + " joined on a VPN/Proxy (" + result.getMethod() + ")");

                            if(AntiVPN.getInstance().getVpnConfig().alertToStaff()) //Ensuring the user wishes to alert to staff
                                AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                                        .filter(APIPlayer::isAlertsEnabled)
                                        .forEach(pl -> pl.sendMessage(AntiVPN.getInstance().getVpnConfig()
                                                .alertMessage()
                                                .replace("%player%", event.getPlayer().getName())
                                                .replace("%reason%", result.getMethod())
                                                .replace("%country%", result.getCountryName())
                                                .replace("%city%", result.getCity())));

                            //In case the user wants to run their own commands instead of using the built in kicking
                            if(AntiVPN.getInstance().getVpnConfig().runCommands()) {
                                for (String command : AntiVPN.getInstance().getVpnConfig().commands()) {
                                    BungeePlugin.pluginInstance.getProxy().getPluginManager()
                                            .dispatchCommand(BungeePlugin.pluginInstance.getProxy().getConsole(),
                                                    ChatColor.translateAlternateColorCodes('&',
                                                            command.replace("%player%", event.getPlayer().getName())));
                                }
                            }
                            AntiVPN.getInstance().detections++;
                        }

                    } else {
                        BungeePlugin.pluginInstance.getProxy().getLogger()
                                .log(Level.WARNING,
                                        "The API query was not a success! " +
                                                "You may need to upgrade your license on https://funkemunky.cc/shop");
                    }
                    AntiVPN.getInstance().checked++;
                });
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        AntiVPN.getInstance().getPlayerExecutor().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
