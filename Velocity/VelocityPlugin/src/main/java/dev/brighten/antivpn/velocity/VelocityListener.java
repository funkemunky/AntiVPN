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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.logging.Level;

public class VelocityListener extends VPNExecutor {

    @Override
    public void registerListeners() {
        VelocityPlugin.INSTANCE.getServer().getEventManager()
                .register(VelocityPlugin.INSTANCE.getPluginInstance(), this);

        VelocityPlugin.INSTANCE.getServer().getEventManager().register(VelocityPlugin.INSTANCE.getPluginInstance(), DisconnectEvent.class,
                event -> AntiVPN.getInstance()
                        .getPlayerExecutor()
                        .unloadPlayer(event.getPlayer().getUniqueId()));

        VelocityPlugin.INSTANCE.getServer().getEventManager().register(VelocityPlugin.INSTANCE.getPluginInstance(), LoginEvent.class,
                this::onLogin);
    }

    public void onLogin(LoginEvent event) {
        APIPlayer player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(event.getPlayer().getUniqueId())
                .orElse(new OfflinePlayer(
                        event.getPlayer().getUniqueId(),
                        event.getPlayer().getUsername(),
                        event.getPlayer().getRemoteAddress().getAddress()
                ));

        CheckResult result = player.checkPlayer();

        if(!result.resultType().isShouldBlock()) return;

        if(!AntiVPN.getInstance().getVpnConfig().isKickPlayers()) {
            return;
        }

        switch (result.resultType()) {
            case DENIED_COUNTRY -> event.setResult(ResultedEvent.ComponentResult.denied(
                    LegacyComponentSerializer.builder()
                            .character('&')
                            .build().deserialize(AntiVPN.getInstance().getVpnConfig()
                                    .getCountryVanillaKickReason()
                                    .replace("%player%", event.getPlayer().getUsername())
                                    .replace("%country%", result.response().getCountryName())
                                    .replace("%code%", result.response().getCountryCode()))));
            case DENIED_PROXY -> {
                VelocityPlugin.INSTANCE.getLogger().info(event.getPlayer().getUsername()
                        + " joined on a VPN/Proxy (" + result.response().getMethod() + ")");
                event.setResult(ResultedEvent.ComponentResult.denied(LegacyComponentSerializer.builder()
                        .character('&')
                        .build().deserialize(AntiVPN.getInstance().getVpnConfig()
                                .getKickMessage()
                                .replace("%player%", event.getPlayer().getUsername())
                                .replace("%country%", result.response().getCountryName())
                                .replace("%code%", result.response().getCountryCode()))));
            }
        }
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
        VelocityPlugin.INSTANCE.getServer().getEventManager().unregisterListener(VelocityPlugin.INSTANCE.getPluginInstance(), this);
        VelocityPlugin.INSTANCE.getServer().getCommandManager().unregister("antivpn");
    }
}
