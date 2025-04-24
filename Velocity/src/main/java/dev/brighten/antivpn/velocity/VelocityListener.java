package dev.brighten.antivpn.velocity;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.velocity.util.StringUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class VelocityListener extends VPNExecutor {

    @Override
    public void registerListeners() {
        VelocityPlugin.INSTANCE.getServer().getEventManager()
                .register(VelocityPlugin.INSTANCE, this);

        VelocityPlugin.INSTANCE.getServer().getEventManager().register(VelocityPlugin.INSTANCE, DisconnectEvent.class,
                event -> AntiVPN.getInstance().getPlayerExecutor().unloadPlayer(event.getPlayer().getUniqueId()));

        VelocityPlugin.INSTANCE.getServer().getEventManager().register(VelocityPlugin.INSTANCE, LoginEvent.class,
                event -> {
                    if (event.getResult().isAllowed()) {
                        if (event.getPlayer().hasPermission("antivpn.bypass") //Has bypass permission
                                //Is exempt
                                || AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getUniqueId())
                                //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                                || AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getRemoteAddress()
                                .getAddress().getHostAddress())
                                || AntiVPN.getInstance().getVpnConfig().getPrefixWhitelists().stream()
                                .anyMatch(prefix -> event.getPlayer().getUsername().startsWith(prefix))) return;
                        checkIp(event.getPlayer().getRemoteAddress().getAddress().getHostAddress())
                                .thenAccept(result -> {
                                    if(!result.isSuccess()) {
                                        VelocityPlugin.INSTANCE.getLogger()
                                                .log(Level.WARNING,
                                                        "The API query was not a success! " +
                                                                "You may need to upgrade your license on " +
                                                                "https://funkemunky.cc/shop");
                                    }
                                    // If the countryList() size is zero, no need to check.
                                    // Running country check first
                                    if (!AntiVPN.getInstance().getVpnConfig().countryList().isEmpty()
                                            && !(AntiVPN.getInstance().getExecutor()
                                            .isWhitelisted(event.getPlayer().getUniqueId()) //Is exempt
                                            //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                                            || AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer()
                                            .getRemoteAddress().getAddress().getHostAddress()))
                                            // This bit of code will decide whether or not to kick the player
                                            // If it contains the code and it is set to whitelist, it will not kick
                                            // as they are equal and vise versa. However, if the contains does not match
                                            // the state, it will kick.
                                            && AntiVPN.getInstance().getVpnConfig().countryList()
                                            .contains(result.getCountryCode())
                                            != AntiVPN.getInstance().getVpnConfig().whitelistCountries()) {
                                        //Using our built in kicking system if no commands are configured
                                        if (AntiVPN.getInstance().getVpnConfig().countryKickCommands().isEmpty()) {
                                            final String kickReason = AntiVPN.getInstance().getVpnConfig()
                                                    .countryVanillaKickReason();
                                            // Kicking our player
                                            event.setResult(ResultedEvent.ComponentResult.denied(LegacyComponentSerializer.builder()
                                                    .character('&')
                                                    .build().deserialize(kickReason
                                                            .replace("%player%", event.getPlayer().getUsername())
                                                            .replace("%country%", result.getCountryName())
                                                            .replace("%code%", result.getCountryCode()))));
                                            VelocityPlugin.INSTANCE.getServer().getScheduler()
                                                    .buildTask(VelocityPlugin.INSTANCE, () ->
                                                            event.getPlayer().disconnect(LegacyComponentSerializer.builder()
                                                                    .character('&')
                                                                    .build().deserialize(kickReason
                                                                            .replace("%player%", event.getPlayer().getUsername())
                                                                            .replace("%country%", result.getCountryName())
                                                                            .replace("%code%", result.getCountryCode()))));
                                        } else {
                                            for (String cmd : AntiVPN.getInstance().getVpnConfig().countryKickCommands()) {
                                                final String formattedCommand = StringUtils
                                                        .translateAlternateColorCodes('&',
                                                                cmd.replace("%player%",
                                                                                event.getPlayer().getUsername())
                                                                        .replace("%country%", result.getCountryName())
                                                                        .replace("%code%", result.getCountryCode()));
                                                // Running the command from console
                                                VelocityPlugin.INSTANCE.getServer().getCommandManager()
                                                        .executeAsync(VelocityPlugin.INSTANCE.getServer()
                                                                        .getConsoleCommandSource(),
                                                                StringUtils.translateAlternateColorCodes('&',
                                                                        formattedCommand));
                                            }
                                        }
                                    } else if (result.isProxy()) {
                                        if (AntiVPN.getInstance().getVpnConfig().kickPlayersOnDetect()) {
                                            // Delay code execution
                                            event.setResult(ResultedEvent.ComponentResult.denied(LegacyComponentSerializer.builder()
                                                    .character('&')
                                                    .build().deserialize(AntiVPN.getInstance().getVpnConfig()
                                                            .getKickString()
                                                            .replace("%player%", event.getPlayer().getUsername())
                                                            .replace("%country%", result.getCountryName())
                                                            .replace("%code%", result.getCountryCode()))));

                                            VelocityPlugin.INSTANCE.getServer().getScheduler()
                                                    .buildTask(VelocityPlugin.INSTANCE, () ->
                                                            event.getPlayer().disconnect(LegacyComponentSerializer.builder()
                                                                    .character('&')
                                                                    .build().deserialize(AntiVPN.getInstance().getVpnConfig()
                                                                            .getKickString()
                                                                            .replace("%player%", event.getPlayer().getUsername())
                                                                            .replace("%country%", result.getCountryName())
                                                                            .replace("%code%", result.getCountryCode()))))
                                                    .delay(1, TimeUnit.SECONDS).schedule();
                                        }
                                        VelocityPlugin.INSTANCE.getLogger().info(event.getPlayer().getUsername()
                                                + " joined on a VPN/Proxy (" + result.getMethod() + ")");
                                        //Ensuring the user wishes to alert to staff
                                        if (AntiVPN.getInstance().getVpnConfig().alertToStaff())
                                            AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                                                    .filter(APIPlayer::isAlertsEnabled)
                                                    .forEach(pl ->
                                                            pl.sendMessage(AntiVPN.getInstance().getVpnConfig()
                                                                    .alertMessage()
                                                                    .replace("%player%",
                                                                            event.getPlayer().getUsername())
                                                                    .replace("%reason%",
                                                                            result.getMethod())
                                                                    .replace("%country%",
                                                                            result.getCountryName())
                                                                    .replace("%city%",
                                                                            result.getCity())));

                                        //In case the user wants to run their own commands instead of using the
                                        // built in kicking
                                        if (AntiVPN.getInstance().getVpnConfig().runCommands()) {
                                            for (String command : AntiVPN.getInstance().getVpnConfig().commands()) {
                                                VelocityPlugin.INSTANCE.getServer().getCommandManager()
                                                        .executeAsync(VelocityPlugin.INSTANCE.getServer()
                                                                        .getConsoleCommandSource(),
                                                                StringUtils.translateAlternateColorCodes('&',
                                                                        command.replace("%player%",
                                                                                event.getPlayer().getUsername())));
                                            }
                                        }
                                        AntiVPN.getInstance().detections++;
                                    }
                                    AntiVPN.getInstance().checked++;
                                });
                    }
                });
    }

    @Override
    public void log(Level level, String log, Object... objects) {
        VelocityPlugin.INSTANCE.getLogger().log(level, String.format(log, objects));
    }

    @Override
    public void log(String log, Object... objects) {
        log(Level.INFO, String.format(log, objects));
    }

    @Override
    public void logException(String message, Throwable ex) {
        VelocityPlugin.INSTANCE.getLogger().log(Level.SEVERE, message, ex);
    }

    @Override
    public void disablePlugin() {
        VelocityPlugin.INSTANCE.getServer().getEventManager().unregisterListener(VelocityPlugin.INSTANCE, this);
        VelocityPlugin.INSTANCE.getServer().getCommandManager().unregister("antivpn");
    }
}
