package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
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

    @Override
    public void log(String log, Object... objects) {
        BungeeCord.getInstance().getLogger().log(Level.INFO, String.format(log, objects));
    }

    @EventHandler
    public void onListener(final PostLoginEvent event) {
        if(event.getPlayer().hasPermission("antivpn.bypass") //Has bypass permission
                || AntiVPN.getInstance().getVpnConfig().getPrefixWhitelists().stream()
                .anyMatch(prefix -> event.getPlayer().getName().startsWith(prefix))) return;

        checkIp(event.getPlayer().getAddress().getAddress().getHostAddress(),
                AntiVPN.getInstance().getVpnConfig().cachedResults(), result -> {
            if(result.isSuccess()) {
                // If the countryList() size is zero, no need to check.
                // Running country check first
                if(AntiVPN.getInstance().getVpnConfig().countryList().size() > 0
                        && !(AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getUniqueId()) //Is exempt
                        //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                        || AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getAddress().getAddress()
                        .getHostAddress()))
                        // This bit of code will decide whether or not to kick the player
                        // If it contains the code and it is set to whitelist, it will not kick as they are equal
                        // and vise versa. However, if the contains does not match the state, it will kick.
                        && AntiVPN.getInstance().getVpnConfig().countryList()
                        .contains(result.getCountryCode()) != AntiVPN.getInstance().getVpnConfig().whitelistCountries()) {
                    //Using our built in kicking system if no commands are configured
                    if(AntiVPN.getInstance().getVpnConfig().countryKickCommands().size() == 0) {
                        final String kickReason = AntiVPN.getInstance().getVpnConfig()
                                .countryVanillaKickReason();
                        // Kicking our player
                        event.getPlayer().disconnect(TextComponent.fromLegacyText(ChatColor
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
                            BungeeCord.getInstance().getPluginManager().dispatchCommand(
                                    BungeeCord.getInstance().getConsole(), formattedCommand);
                        }
                    }
                } else if(result.isProxy()) {
                    if(AntiVPN.getInstance().getVpnConfig().kickPlayersOnDetect())
                        event.getPlayer().disconnect(TextComponent.fromLegacyText(ChatColor
                                .translateAlternateColorCodes('&',
                                        AntiVPN.getInstance().getVpnConfig().getKickString())));
                    BungeeCord.getInstance().getLogger().info(event.getPlayer().getName()
                            + " joined on a VPN/Proxy (" + result.getMethod() + ")");

                    if(AntiVPN.getInstance().getVpnConfig().alertToStaff()) //Ensuring the user wishes to alert to staff
                        AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                                .filter(APIPlayer::isAlertsEnabled)
                                .forEach(pl -> pl.sendMessage(AntiVPN.getInstance().getVpnConfig().alertMessage()
                                        .replace("%player%", event.getPlayer().getName())
                                        .replace("%reason%", result.getMethod())
                                        .replace("%country%", result.getCountryName())
                                        .replace("%city%", result.getCity())));

                    //In case the user wants to run their own commands instead of using the built in kicking
                    if(AntiVPN.getInstance().getVpnConfig().runCommands()) {
                        for (String command : AntiVPN.getInstance().getVpnConfig().commands()) {
                            BungeeCord.getInstance().getPluginManager()
                                    .dispatchCommand(BungeeCord.getInstance().getConsole(),
                                            ChatColor.translateAlternateColorCodes('&',
                                                    command.replace("%player%", event.getPlayer().getName())));
                        }
                    }
                    AntiVPN.getInstance().detections++;
                }

            } else {
                BungeeCord.getInstance().getLogger()
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
