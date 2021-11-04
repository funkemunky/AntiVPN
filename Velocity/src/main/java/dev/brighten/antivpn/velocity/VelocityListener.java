package dev.brighten.antivpn.velocity;

import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.scheduler.ScheduledTask;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.velocity.util.StringUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class VelocityListener extends VPNExecutor {

    private ScheduledTask cacheResetTask;

    @Override
    public void registerListeners() {
        VelocityPlugin.INSTANCE.getServer().getEventManager()
                .register(VelocityPlugin.INSTANCE, this);

        VelocityPlugin.INSTANCE.getServer().getEventManager().register(VelocityPlugin.INSTANCE, LoginEvent.class,
                event -> {
            if(event.getResult().isAllowed()) {
                if(event.getPlayer().hasPermission("antivpn.bypass") //Has bypass permission
                        || AntiVPN.getInstance().getExecutor().isWhitelisted(event.getPlayer().getUniqueId()) //Is exempt
                        //Or has a name that starts with a certain prefix. This is for Bedrock exempting.
                        || AntiVPN.getInstance().getConfig().getPrefixWhitelists().stream()
                        .anyMatch(prefix -> event.getPlayer().getUsername().startsWith(prefix))) return;

                checkIp(event.getPlayer().getRemoteAddress().getAddress().getHostAddress(),
                        AntiVPN.getInstance().getConfig().cachedResults(), result -> {
                            if(result.isSuccess() && result.isProxy()) {
                                if(AntiVPN.getInstance().getConfig().kickPlayersOnDetect())
                                    event.getPlayer().disconnect(LegacyComponentSerializer.builder().character('&')
                                            .build().deserialize(AntiVPN.getInstance().getConfig().getKickString()));
                                VelocityPlugin.INSTANCE.getLogger().info(event.getPlayer().getUsername()
                                        + " joined on a VPN/Proxy (" + result.getMethod() + ")");

                                if(AntiVPN.getInstance().getConfig().alertToStaff()) //Ensuring the user wishes to alert to staff
                                    AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                                            .filter(APIPlayer::isAlertsEnabled)
                                            .forEach(pl -> pl.sendMessage(AntiVPN.getInstance().getConfig().alertMessage()
                                                    .replace("%player%", event.getPlayer().getUsername())
                                                    .replace("%reason%", result.getMethod())
                                                    .replace("%country%", result.getCountryName())
                                                    .replace("%city%", result.getCity())));

                                //In case the user wants to run their own commands instead of using the built in kicking
                                if(AntiVPN.getInstance().getConfig().runCommands()) {
                                    for (String command : AntiVPN.getInstance().getConfig().commands()) {
                                        VelocityPlugin.INSTANCE.getServer().getCommandManager()
                                                .executeAsync(VelocityPlugin.INSTANCE.getServer()
                                                                .getConsoleCommandSource(),
                                                        StringUtils.translateAlternateColorCodes('&',
                                                                command.replace("%player%",
                                                                        event.getPlayer().getUsername())));
                                    }
                                }
                                AntiVPN.getInstance().detections++;
                            } else if(!result.isSuccess()) {
                                VelocityPlugin.INSTANCE.getLogger()
                                        .log(Level.WARNING,
                                                "The API query was not a success! " +
                                                        "You may need to upgrade your license on https://funkemunky.cc/shop");
                            }
                            AntiVPN.getInstance().checked++;
                        });
            }
        });
    }

    @Override
    public void runCacheReset() {
        cacheResetTask = VelocityPlugin.INSTANCE.getServer().getScheduler()
                .buildTask(VelocityPlugin.INSTANCE, this::resetCache)
                .repeat(20, TimeUnit.MINUTES)
                .schedule();
    }

    @Override
    public void shutdown() {
        if(cacheResetTask != null) {
            cacheResetTask.cancel();
            cacheResetTask = null;
        }
        threadExecutor.shutdown();
        VelocityPlugin.INSTANCE.getServer().getEventManager().unregisterListener(VelocityPlugin.INSTANCE, this);
    }

    @Override
    public void log(String log, Object... objects) {
        VelocityPlugin.INSTANCE.getLogger().log(Level.INFO, String.format(log, objects));
    }
}
