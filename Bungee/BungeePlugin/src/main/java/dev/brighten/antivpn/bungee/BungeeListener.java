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

package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.*;
import dev.brighten.antivpn.utils.MiscUtils;
import dev.brighten.antivpn.utils.StringUtil;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
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
                .registerListener(BungeePlugin.pluginInstance.getPlugin(), this);
    }

    @Override
    public void log(Level level, String log, Object... objects) {
        BungeePlugin.pluginInstance.getProxy().getLogger().log(Level.INFO, String.format(log, objects));
    }

    @Override
    public void log(String log, Object... objects) {
        log(Level.INFO, log, objects);
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
        BungeePlugin.pluginInstance.getProxy().getPluginManager().unregisterListeners(BungeePlugin.pluginInstance.getPlugin());
        if (cacheResetTask != null) {
            cacheResetTask.cancel();
            cacheResetTask = null;
        }
        BungeePlugin.pluginInstance.getProxy().getPluginManager().unregisterCommands(BungeePlugin.pluginInstance.getPlugin());
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

        CheckResult result = player.checkPlayer();

        if (!result.resultType().isShouldBlock()) return;

        if(!AntiVPN.getInstance().getVpnConfig().isKickPlayers()) {
            return;
        }

        event.setCancelled(true);
        event.setReason(TextComponent.fromLegacy(StringUtil.varReplace(switch (result.resultType()) {
            case DENIED_PROXY -> StringUtil.varReplace(AntiVPN.getInstance().getVpnConfig()
                    .getKickMessage(), player, result.response());
            case DENIED_COUNTRY -> StringUtil.varReplace(AntiVPN.getInstance().getVpnConfig()
                    .getCountryVanillaKickReason(), player, result.response());
            default -> "You were kicked by KauriVPN for an unknown reason!";
        }, player, result.response())));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(LoginEvent event) {
        if(event.isCancelled()) return;

        // Handling player alerts on join
        AntiVPN.getInstance().getPlayerExecutor().getPlayer(event.getConnection().getUniqueId())
                .ifPresent(APIPlayer::checkAlertsState);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        AntiVPN.getInstance().getPlayerExecutor().unloadPlayer(event.getPlayer().getUniqueId());
    }
}
