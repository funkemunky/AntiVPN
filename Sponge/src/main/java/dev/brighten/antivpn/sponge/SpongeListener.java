package dev.brighten.antivpn.sponge;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.*;
import dev.brighten.antivpn.sponge.util.StringUtil;
import dev.brighten.antivpn.utils.Tuple;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class SpongeListener extends VPNExecutor {

    @Listener(order = Order.EARLY)
    public void onJoin(ServerSideConnectionEvent.Auth event) {
        AtomicReference<APIPlayer> player = new AtomicReference<>(AntiVPN.getInstance().getPlayerExecutor()
                .getPlayer(event.profile().uuid())
                .orElse(new OfflinePlayer(
                        event.profile().uuid(),
                        event.profile().name().orElse("Unknown"),
                        event.connection().address().getAddress()
                )));

        CheckResult instantResult = player.get().checkPlayer(result -> {
            if(result.resultType().isShouldBlock()) {
                AntiVPN.getInstance().getExecutor().getToKick().add(new Tuple<>(result, player.get().getUuid()));
            }
        });

        if(!instantResult.resultType().isShouldBlock()) {
            return;
        }

        AntiVPN.getInstance().getExecutor().getToKick().add(new Tuple<>(instantResult, player.get().getUuid()));

        if(!AntiVPN.getInstance().getVpnConfig().kickPlayersOnDetect()) {
            return;
        }

        event.setCancelled(true);
        switch (instantResult.resultType()) {
            case DENIED_PROXY -> {
                AntiVPN.getInstance().getExecutor().log(Level.INFO, player.get().getName()
                        + " joined on a VPN/Proxy (" + instantResult.response().getMethod() + ")");
                event.setMessage(Component.text(StringUtil
                        .translateColorCodes('&', AntiVPN.getInstance().getVpnConfig()
                                .getKickString()
                                .replace("%player%", player.get().getName())
                                .replace("%country%", instantResult.response().getCountryName())
                                .replace("%code%", instantResult.response().getCountryCode()))));
            }
            case DENIED_COUNTRY ->
                    event.setMessage(Component.text(StringUtil
                            .translateColorCodes('&', AntiVPN.getInstance().getVpnConfig()
                                    .countryVanillaKickReason()
                                    .replace("%player%", player.get().getName())
                                    .replace("%country%", instantResult.response().getCountryName())
                                    .replace("%code%", instantResult.response().getCountryCode()))));
        }
    }

    @Override
    public void registerListeners() {
        Sponge.eventManager().registerListeners(SpongePlugin.getInstance().getContainer(), this);
    }

    @Override
    public void log(Level level, String log, Object... objects) {
        if (level.equals(Level.SEVERE)) {
            SpongePlugin.getInstance().getLogger().error(String.format(log, objects));
        } else if (level.equals(Level.WARNING)) {
            SpongePlugin.getInstance().getLogger().warn(String.format(log, objects));
        } else {
            SpongePlugin.getInstance().getLogger().info(String.format(log, objects));
        }
    }

    @Override
    public void log(String log, Object... objects) {
        log(Level.INFO, String.format(log, objects));
    }

    @Override
    public void logException(String message, Throwable ex) {
        SpongePlugin.getInstance().getLogger().error(message, ex);
    }

    @Override
    public void runCommand(String command) {
        try {
            Sponge.server().commandManager().process(Sponge.systemSubject(), command);
        } catch (CommandException e) {
            logException(e);
        }
    }

    @Override
    public void disablePlugin() {
        AntiVPN.getInstance().getExecutor().log(Level.INFO, "Disabling listeners for plugin...");
    }
}
