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

package dev.brighten.antivpn.api;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.CIDRUtils;
import dev.brighten.antivpn.utils.StringUtil;
import dev.brighten.antivpn.utils.Tuple;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.web.FunkemunkyAPI;
import dev.brighten.antivpn.web.objects.VPNResponse;
import lombok.Getter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

@Getter
public abstract class VPNExecutor {
    private final ScheduledExecutorService threadExecutor = Executors.newScheduledThreadPool(2);
    private final Set<UUID> whitelisted = Collections.synchronizedSet(new HashSet<>());
    private final Set<CIDRUtils> whitelistedIps = Collections.synchronizedSet(new HashSet<>());
    private final Queue<Tuple<CheckResult, UUID>> toKick = new LinkedBlockingQueue<>();
    private final Queue<APIPlayer> playersToRecheck = new LinkedBlockingQueue<>();
    private ScheduledFuture<?> kickTask = null;


    public abstract void registerListeners();

    public abstract void log(Level level, String log, Object... objects);

    public abstract void log(String log, Object... objects);

    public abstract void logException(String message, Throwable ex);

    public abstract void runCommand(String command);

    public void logException(Throwable ex) {
        logException("An exception occurred: " + ex.getMessage(), ex);
    }

    public void startKickChecks() {
        kickTask = threadExecutor.scheduleAtFixedRate(() -> {
            synchronized (toKick) {
                if(toKick.isEmpty()) return;

                Tuple<CheckResult, UUID> toCheck;

                while((toCheck = toKick.poll()) != null) {
                    Optional<APIPlayer> player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(toCheck.second());

                    if(player.isEmpty()) {
                        continue;
                    }

                    handleKickingOfPlayer(toCheck.first(), player.get());
                }
            }
        }, 8, 2, TimeUnit.SECONDS);
    }

    public void handleKickingOfPlayer(CheckResult result, APIPlayer player) {

        //Ensuring kick task is always running
        if(kickTask == null || kickTask.isDone() || kickTask.isCancelled()) {
            startKickChecks();
        }

        if (AntiVPN.getInstance().getVpnConfig().isAlertToSTaff()) AntiVPN.getInstance().getPlayerExecutor()
                .getOnlinePlayers()
                .stream()
                .filter(APIPlayer::isAlertsEnabled)
                .forEach(pl ->
                        pl.sendMessage(StringUtil.translateAlternateColorCodes('&',
                                StringUtil.varReplace(dev.brighten.antivpn.AntiVPN.getInstance().getVpnConfig()
                                        .getAlertMsg(), player, result.response()))));

        if(AntiVPN.getInstance().getVpnConfig().isKickPlayers()) {
            switch (result.resultType()) {
                case DENIED_PROXY -> player.kickPlayer(StringUtil.varReplace(AntiVPN.getInstance().getVpnConfig()
                        .getKickMessage(), player, result.response()));
                case DENIED_COUNTRY -> player.kickPlayer(StringUtil.varReplace(AntiVPN.getInstance().getVpnConfig()
                        .getCountryVanillaKickReason(), player, result.response()));
            }
        } else {
            if(!AntiVPN.getInstance().getVpnConfig().isCommandsEnabled()) return;
        }

        Runnable runCommands = () -> {
            switch (result.resultType()) {
                case DENIED_PROXY -> {
                    for (String command : AntiVPN.getInstance().getVpnConfig().commands()) {
                        runCommand(StringUtil.varReplace(command, player, result.response()));
                    }
                }
                case DENIED_COUNTRY -> {
                    for (String command : AntiVPN.getInstance().getVpnConfig().countryKickCommands()) {
                        runCommand(StringUtil.varReplace(command, player, result.response()));
                    }
                }
            }
        };

        // Fixes the commands running too fast and causing messaging errors by any downstream plugins like LiteBans
        var scheduleResult = threadExecutor.schedule(runCommands, 1, TimeUnit.SECONDS);

        if(scheduleResult.isCancelled()) {
            runCommands.run();
        }

        //Ensuring players are actually kicked as they are supposed to be.
        toKick.add(new Tuple<>(result, player.getUuid()));
    }

    public boolean isWhitelisted(UUID uuid) {
        if(AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled()) {
            return AntiVPN.getInstance().getDatabase().isWhitelisted(uuid);
        }
        return whitelisted.contains(uuid);
    }

    public boolean isWhitelisted(String cidr) {
        if(AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled()) {
            return AntiVPN.getInstance().getDatabase().isWhitelisted(cidr);
        }
        try {
            return whitelistedIps.contains(new CIDRUtils(cidr));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private final Cache<String, VPNResponse> cachedResponses = Caffeine.newBuilder()
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .maximumSize(4000)
            .build();

    public CompletableFuture<VPNResponse> checkIp(String ip) {
        VPNResponse cached = cachedResponses.getIfPresent(ip);

        if(cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            Optional<VPNResponse> cachedRes = AntiVPN.getInstance().getDatabase().getStoredResponse(ip);

            if(cachedRes.isPresent()) {
                return cachedRes.get();
            }
            else {
                try {
                    VPNResponse response = FunkemunkyAPI
                            .getVPNResponse(ip, AntiVPN.getInstance().getVpnConfig().getLicense(), true);

                    if (response.isSuccess()) {
                        AntiVPN.getInstance().getDatabase().cacheResponse(response);
                    } else {
                        log("Query to VPN API failed! Reason: " + response.getFailureReason());
                    }

                    return response;
                } catch (JSONException | IOException e) {
                    log("Query to VPN API failed! Reason: " + e.getMessage());
                    return VPNResponse.FAILED_RESPONSE;
                }
            }
        }, threadExecutor);
    }

    public abstract void disablePlugin();
}
