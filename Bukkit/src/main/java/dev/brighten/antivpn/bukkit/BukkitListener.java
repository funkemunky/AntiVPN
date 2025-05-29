package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.CheckResult;
import dev.brighten.antivpn.api.OfflinePlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.message.VpnString;
import dev.brighten.antivpn.utils.StringUtil;
import dev.brighten.antivpn.utils.Tuple;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

@SuppressWarnings("unchecked")
public class BukkitListener extends VPNExecutor implements Listener {

    @Override
    public void registerListeners() {
        BukkitPlugin.pluginInstance.getServer().getPluginManager()
                .registerEvents(this, BukkitPlugin.pluginInstance);
    }

    @Override
    public void log(Level level, String log, Object... objects) {
        Bukkit.getLogger().log(level, String.format(log, objects));
    }

    @Override
    public void log(String log, Object... objects) {
        log(Level.INFO, String.format(log, objects));
    }

    @Override
    public void logException(String message, Throwable ex) {
        Bukkit.getLogger().log(Level.SEVERE, message, ex);
    }

    @Override
    public void runCommand(String command) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                ChatColor.translateAlternateColorCodes('&', command));
    }

    @Override
    public void disablePlugin() {
        HandlerList.unregisterAll(this);
        BukkitPlugin.pluginInstance.getServer().getPluginManager().disablePlugin(BukkitPlugin.pluginInstance);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(final PlayerLoginEvent event) {
        APIPlayer player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(event.getPlayer().getUniqueId())
                .orElse(new OfflinePlayer(
                        event.getPlayer().getUniqueId(),
                        event.getPlayer().getName(),
                        event.getAddress()
                ));

        CheckResult instantResult = player.checkPlayer(result -> {
            if(!result.resultType().isShouldBlock()) return;

            AntiVPN.getInstance().getExecutor().log(Level.INFO, "Adding %s to kick", event.getPlayer().getName());
            AntiVPN.getInstance().getExecutor().getToKick().add(new Tuple<>(result, event.getPlayer().getUniqueId()));
        });

        if(!instantResult.resultType().isShouldBlock()) return;

        AntiVPN.getInstance().getExecutor().getToKick()
                .add(new Tuple<>(instantResult, event.getPlayer().getUniqueId()));

        if(!AntiVPN.getInstance().getVpnConfig().kickPlayersOnDetect()) {
            return;
        }

        AntiVPN.getInstance().getExecutor().log(Level.INFO, "%s was kicked from pre-login cache with IP %s", event.getPlayer().getName(), instantResult.response().getIp());

        event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
        switch (instantResult.resultType()) {
            case DENIED_COUNTRY -> event.setKickMessage(StringUtil.translateAlternateColorCodes('&',
                    StringUtil.varReplace(
                            AntiVPN.getInstance().getVpnConfig().countryVanillaKickReason(),
                            player,
                            instantResult.response()
                    )));
            case DENIED_PROXY -> {
                if(AntiVPN.getInstance().getVpnConfig().alertToStaff()) {
                    AntiVPN.getInstance().getPlayerExecutor().getOnlinePlayers().stream()
                            .filter(APIPlayer::isAlertsEnabled)
                            .forEach(pl ->
                                    pl.sendMessage(StringUtil.varReplace(
                                            ChatColor.translateAlternateColorCodes(
                                                    '&',
                                                    AntiVPN.getInstance().getVpnConfig().alertMessage()),
                                            player,
                                            instantResult.response())));
                }
                event.setKickMessage(StringUtil.translateAlternateColorCodes('&',
                        StringUtil.varReplace(
                                AntiVPN.getInstance().getVpnConfig().getKickString(),
                                player,
                                instantResult.response()
                        )));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        AntiVPN.getInstance().getPlayerExecutor().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(player -> {
                    AntiVPN.getInstance().getExecutor().getThreadExecutor().execute(() -> {
                        if(AntiVPN.getInstance().getDatabase().getAlertsState(player.getUuid())) {
                            player.setAlertsEnabled(true);
                            player.sendMessage(AntiVPN.getInstance().getMessageHandler()
                                    .getString("command-alerts-toggled")
                                    .getFormattedMessage(new VpnString.Var<>("state", true)));
                        }
                    });
                });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        AntiVPN.getInstance().getPlayerExecutor().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
