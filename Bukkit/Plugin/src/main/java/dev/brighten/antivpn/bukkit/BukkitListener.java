/*
 * Copyright 2026 Dawson Hessler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.OfflinePlayer;
import dev.brighten.antivpn.api.VPNExecutor;
import dev.brighten.antivpn.utils.StringUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class BukkitListener extends VPNExecutor implements Listener {

    @Override
    public void registerListeners() {
        Bukkit.getPluginManager()
                .registerEvents(this, BukkitPlugin.pluginInstance.getPlugin());
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
        new BukkitRunnable() {
            public void run() {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                        ChatColor.translateAlternateColorCodes('&', command));
            }
        }.runTask(BukkitPlugin.pluginInstance.getPlugin());
    }

    @Override
    public void disablePlugin() {
        HandlerList.unregisterAll(this);
        Bukkit.getPluginManager().disablePlugin(BukkitPlugin.pluginInstance.getPlugin());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(final PlayerLoginEvent event) {
        APIPlayer player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(event.getPlayer().getUniqueId())
                .orElse(new OfflinePlayer(
                        event.getPlayer().getUniqueId(),
                        event.getPlayer().getName(),
                        event.getAddress()
                ));

        player.checkPlayer(result -> {
            if(!result.resultType().isShouldBlock()) return;

            if(!AntiVPN.getInstance().getVpnConfig().isKickPlayers()) {
                return;
            }

            event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
            event.setKickMessage(switch (result.resultType()) {
                case DENIED_COUNTRY -> StringUtil.varReplace(
                        AntiVPN.getInstance().getVpnConfig().getCountryVanillaKickReason(),
                        player,
                        result.response()
                );
                case DENIED_PROXY ->
                        StringUtil.varReplace(
                                AntiVPN.getInstance().getVpnConfig().getKickMessage(),
                                player,
                                result.response()
                        );
                default -> "You were kicked by KauriVPN for an unknown reason!";
            });
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(final PlayerJoinEvent event) {
        AntiVPN.getInstance().getPlayerExecutor().getPlayer(event.getPlayer().getUniqueId())
                .ifPresent(APIPlayer::checkAlertsState);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        AntiVPN.getInstance().getPlayerExecutor().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
