package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.CheckResult;
import dev.brighten.antivpn.api.OfflinePlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.utils.MiscUtils;
import dev.brighten.antivpn.utils.StringUtil;
import dev.brighten.antivpn.utils.Tuple;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.logging.Level;

public class BungeeListener extends VPNExecutor implements Listener {

    private ScheduledTask cacheResetTask;

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
    public void runCommand(String command) {
        BungeePlugin.pluginInstance.getProxy().getPluginManager()
                .dispatchCommand(BungeePlugin.pluginInstance.getProxy().getConsole(), command);
    }

    @Override
    public void disablePlugin() {
        BungeePlugin.pluginInstance.getProxy().getPluginManager().unregisterListeners(BungeePlugin.pluginInstance);
        if (cacheResetTask != null) {
            cacheResetTask.cancel();
            cacheResetTask = null;
        }
        BungeePlugin.pluginInstance.getProxy().getPluginManager().unregisterCommands(BungeePlugin.pluginInstance);
        BungeePlugin.pluginInstance.onDisable();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onListener(final PreLoginEvent event) {

        APIPlayer player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(event.getConnection().getUniqueId())
                .orElseGet(() -> {
                    UUID uuid = MiscUtils.lookupUUID(event.getConnection().getName());
                    AntiVPN.getInstance().getExecutor().log(Level.INFO, "Getting offline player for %s with name %s",
                            event.getConnection().getUniqueId(), uuid);

                    return new OfflinePlayer(uuid, event.getConnection().getName(),
                            ((InetSocketAddress) event.getConnection().getSocketAddress()).getAddress());
                });

        CheckResult instantResult = player.checkPlayer(result -> {
            if (!result.resultType().isShouldBlock()) return;
            AntiVPN.getInstance().getExecutor().getToKick()
                    .add(new Tuple<>(result, event.getConnection().getUniqueId()));
        });

        if (!instantResult.resultType().isShouldBlock()) {
            return;
        }

        AntiVPN.getInstance().getExecutor().getToKick()
                .add(new Tuple<>(instantResult, player.getUuid()));

        if (!AntiVPN.getInstance().getVpnConfig().kickPlayersOnDetect()) {
            return;
        }

        event.setCancelled(true);
        AntiVPN.getInstance().getExecutor().log(Level.INFO,
                "%s was kicked from pre-login proxy cache.",
                event.getConnection().getName());


        switch (instantResult.resultType()) {
            case DENIED_PROXY -> event.setReason(TextComponent.fromLegacy(ChatColor
                    .translateAlternateColorCodes('&',
                            StringUtil.varReplace(
                                    AntiVPN.getInstance().getVpnConfig().getKickString(),
                                    player,
                                    instantResult.response()))));
            case DENIED_COUNTRY -> event.setReason(TextComponent.fromLegacy(ChatColor
                    .translateAlternateColorCodes('&',
                            StringUtil.varReplace(
                                    AntiVPN.getInstance().getVpnConfig().countryVanillaKickReason(),
                                    player,
                                    instantResult.response()))));
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        AntiVPN.getInstance().getPlayerExecutor().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
