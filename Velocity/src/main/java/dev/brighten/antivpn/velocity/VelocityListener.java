package dev.brighten.antivpn.velocity;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.CheckResult;
import dev.brighten.antivpn.api.OfflinePlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.utils.StringUtil;
import dev.brighten.antivpn.web.objects.VPNResponse;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class VelocityListener extends VPNExecutor {

    @Override
    public void registerListeners() {
        VelocityPlugin.INSTANCE.getServer().getEventManager()
                .register(VelocityPlugin.INSTANCE, this);

        VelocityPlugin.INSTANCE.getServer().getEventManager().register(VelocityPlugin.INSTANCE, DisconnectEvent.class,
                event -> AntiVPN.getInstance()
                        .getPlayerExecutor()
                        .unloadPlayer(event.getPlayer().getUniqueId()));

        VelocityPlugin.INSTANCE.getServer().getEventManager().register(VelocityPlugin.INSTANCE, LoginEvent.class,
                event -> {
            APIPlayer player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(event.getPlayer().getUniqueId())
                    .orElse(new OfflinePlayer(
                            event.getPlayer().getUniqueId(),
                            event.getPlayer().getUsername(),
                            event.getPlayer().getRemoteAddress().getAddress()
                    ));

            CheckResult instantResult = player.checkPlayer(result -> {
                if(!result.resultType().isShouldBlock()) return;

                handleDeniedTasks(event, result);
            });

            if(!instantResult.resultType().isShouldBlock()) return;

            switch (instantResult.resultType()) {
                case DENIED_COUNTRY -> event.setResult(ResultedEvent.ComponentResult.denied(
                        LegacyComponentSerializer.builder()
                                .character('&')
                                .build().deserialize(AntiVPN.getInstance().getVpnConfig()
                                        .countryVanillaKickReason()
                                        .replace("%player%", event.getPlayer().getUsername())
                                        .replace("%country%", instantResult.response().getCountryName())
                                        .replace("%code%", instantResult.response().getCountryCode()))));
                case DENIED_PROXY -> {
                    VelocityPlugin.INSTANCE.getLogger().info(event.getPlayer().getUsername()
                            + " joined on a VPN/Proxy (" + instantResult.response().getMethod() + ")");
                    event.setResult(ResultedEvent.ComponentResult.denied(LegacyComponentSerializer.builder()
                            .character('&')
                            .build().deserialize(AntiVPN.getInstance().getVpnConfig()
                                    .getKickString()
                                    .replace("%player%", event.getPlayer().getUsername())
                                    .replace("%country%", instantResult.response().getCountryName())
                                    .replace("%code%", instantResult.response().getCountryCode()))));
                }
            }

            handleDeniedTasks(event, instantResult, true);
        });
    }

    private void handleDeniedTasks(LoginEvent event, CheckResult result) {
        handleDeniedTasks(event, result, false);
    }

    private void handleDeniedTasks(LoginEvent event, CheckResult checkResult, boolean deniedOnLogin) {
        VPNResponse result = checkResult.response();
        //Ensuring the user wishes to alert to staff
        if (AntiVPN.getInstance().getVpnConfig().alertToStaff())
            AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                    .filter(APIPlayer::isAlertsEnabled)
                    .forEach(pl ->
                            pl.sendMessage(dev.brighten.antivpn.AntiVPN.getInstance().getVpnConfig()
                                    .alertMessage()
                                    .replace("%player%",
                                            event.getPlayer().getUsername())
                                    .replace("%reason%",
                                            result.getMethod())
                                    .replace("%country%",
                                            result.getCountryName())
                                    .replace("%city%",
                                            result.getCity())));

        if (deniedOnLogin) return;

        //In case the user wants to run their own commands instead of using the
        // built in kicking

        if (AntiVPN.getInstance().getVpnConfig().kickPlayersOnDetect()) {
            switch (checkResult.resultType()) {
                case DENIED_PROXY -> VelocityPlugin.INSTANCE.getServer().getScheduler()
                        .buildTask(VelocityPlugin.INSTANCE, () ->
                                event.getPlayer().disconnect(LegacyComponentSerializer.builder()
                                        .character('&')
                                        .build().deserialize(AntiVPN.getInstance().getVpnConfig()
                                                .getKickString()
                                                .replace("%player%", event.getPlayer().getUsername())
                                                .replace("%country%", result.getCountryName())
                                                .replace("%code%", result.getCountryCode()))))
                        .delay(1, TimeUnit.SECONDS).schedule();
                case DENIED_COUNTRY -> VelocityPlugin.INSTANCE.getServer().getScheduler()
                        .buildTask(VelocityPlugin.INSTANCE, () ->
                                event.getPlayer().disconnect(LegacyComponentSerializer.builder()
                                        .character('&')
                                        .build().deserialize(AntiVPN.getInstance().getVpnConfig()
                                                .countryVanillaKickReason()
                                                .replace("%player%", event.getPlayer().getUsername())
                                                .replace("%country%", result.getCountryName())
                                                .replace("%code%", result.getCountryCode()))))
                        .delay(1, TimeUnit.SECONDS).schedule();
            }
        }

        if (!AntiVPN.getInstance().getVpnConfig().runCommands()) return;

        switch (checkResult.resultType()) {
            case DENIED_PROXY -> {
                for (String command : AntiVPN.getInstance().getVpnConfig().commands()) {
                    VelocityPlugin.INSTANCE.getServer().getCommandManager()
                            .executeAsync(VelocityPlugin.INSTANCE.getServer()
                                            .getConsoleCommandSource(),
                                    StringUtil.translateAlternateColorCodes('&',
                                            StringUtil.varReplace(
                                                    command,
                                                    AntiVPN.getInstance().getPlayerExecutor()
                                                            .getPlayer(event.getPlayer().getUniqueId())
                                                            .orElse(new OfflinePlayer(
                                                                    event.getPlayer().getUniqueId(),
                                                                    event.getPlayer().getUsername(),
                                                                    event.getPlayer().getRemoteAddress().getAddress())
                                                            ),
                                                    result)));
                }
            }
            case DENIED_COUNTRY -> {
                for (String cmd : AntiVPN.getInstance().getVpnConfig().countryKickCommands()) {
                    final String formattedCommand = StringUtil
                            .translateAlternateColorCodes('&',
                                    StringUtil.varReplace(
                                            cmd,
                                            AntiVPN.getInstance().getPlayerExecutor()
                                                    .getPlayer(event.getPlayer().getUniqueId())
                                                    .orElse(new OfflinePlayer(
                                                            event.getPlayer().getUniqueId(),
                                                            event.getPlayer().getUsername(),
                                                            event.getPlayer().getRemoteAddress().getAddress())
                                                    ),
                                            result));
                    // Running the command from console
                    runCommand(formattedCommand);
                }
            }
        }
    }

    @Override
    public void shutdown() {
        VelocityPlugin.INSTANCE.getServer().getEventManager().unregisterListener(VelocityPlugin.INSTANCE, this);
        super.shutdown();
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
    public void runCommand(String command) {
        VelocityPlugin.INSTANCE.getServer().getCommandManager()
                .executeAsync(VelocityPlugin.INSTANCE.getServer()
                                .getConsoleCommandSource(),
                        StringUtil.translateAlternateColorCodes('&',
                                command));
    }

    @Override
    public void disablePlugin() {
        VelocityPlugin.INSTANCE.getServer().getEventManager().unregisterListener(VelocityPlugin.INSTANCE, this);
        VelocityPlugin.INSTANCE.getServer().getCommandManager().unregister("antivpn");
    }
}
