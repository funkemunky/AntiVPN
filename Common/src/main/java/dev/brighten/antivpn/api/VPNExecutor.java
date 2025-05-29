package dev.brighten.antivpn.api;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.StringUtil;
import dev.brighten.antivpn.utils.Tuple;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.web.FunkemunkyAPI;
import dev.brighten.antivpn.web.objects.VPNResponse;
import lombok.Getter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public abstract class VPNExecutor {

    @Getter
    private final ScheduledExecutorService threadExecutor = Executors.newScheduledThreadPool(2);

    @Getter
    private final Set<UUID> whitelisted = Collections.synchronizedSet(new HashSet<>());

    @Getter
    private final Set<String> whitelistedIps = Collections.synchronizedSet(new HashSet<>());

    @Getter
    private final List<Tuple<CheckResult, UUID>> toKick = Collections.synchronizedList(new LinkedList<>());

    public abstract void registerListeners();

    public void shutdown() {
        threadExecutor.shutdown();
    }

    public abstract void log(Level level, String log, Object... objects);

    public abstract void log(String log, Object... objects);

    public abstract void logException(String message, Throwable ex);

    public abstract void runCommand(String command);

    public void logException(Throwable ex) {
        logException("An exception occurred: " + ex.getMessage(), ex);
    }

    public void startKickChecks() {
        threadExecutor.scheduleAtFixedRate(() -> {
            synchronized (toKick) {
                if(toKick.isEmpty()) return;

                Iterator<Tuple<CheckResult, UUID>> i = toKick.iterator();

                while(i.hasNext()) {
                    var toCheck = i.next();

                    Optional<APIPlayer> player = AntiVPN.getInstance().getPlayerExecutor().getPlayer(toCheck.second());

                    if(player.isEmpty()) {
                        continue;
                    }

                    handleKickingOfPlayer(toCheck.first(), player.get());

                    i.remove();
                }
            }
        }, 8, 2, TimeUnit.SECONDS);
    }

    public void handleKickingOfPlayer(CheckResult result, APIPlayer player) {
        if (AntiVPN.getInstance().getVpnConfig().alertToStaff()) AntiVPN.getInstance().getPlayerExecutor()
                .getOnlinePlayers()
                .stream()
                .filter(APIPlayer::isAlertsEnabled)
                .forEach(pl ->
                        pl.sendMessage(StringUtil.translateAlternateColorCodes('&',
                                StringUtil.varReplace(dev.brighten.antivpn.AntiVPN.getInstance().getVpnConfig()
                                        .alertMessage(), player, result.response()))));

        if(AntiVPN.getInstance().getVpnConfig().kickPlayersOnDetect()) {
            switch (result.resultType()) {
                case DENIED_PROXY -> player.kickPlayer(StringUtil.varReplace(AntiVPN.getInstance().getVpnConfig()
                        .getKickString(), player, result.response()));
                case DENIED_COUNTRY -> player.kickPlayer(StringUtil.varReplace(AntiVPN.getInstance().getVpnConfig()
                        .countryVanillaKickReason(), player, result.response()));
            }
        }

        if(!AntiVPN.getInstance().getVpnConfig().runCommands()) return;

        switch (result.resultType()) {
            case DENIED_PROXY -> {
                for (String command : AntiVPN.getInstance().getVpnConfig().commands()) {
                    runCommand(StringUtil.translateAlternateColorCodes('&',
                            StringUtil.varReplace(command, player, result.response())));
                }
            }
            case DENIED_COUNTRY -> {
                for (String command : AntiVPN.getInstance().getVpnConfig().countryKickCommands()) {
                    runCommand(StringUtil.translateAlternateColorCodes('&',
                            StringUtil.varReplace(command, player, result.response())));
                }
            }
        }
    }

    public boolean isWhitelisted(UUID uuid) {
        if(AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled()) {
            return AntiVPN.getInstance().getDatabase().isWhitelisted(uuid);
        }
        return whitelisted.contains(uuid);
    }

    public boolean isWhitelisted(String ip) {
        if(AntiVPN.getInstance().getVpnConfig().isDatabaseEnabled()) {
            return AntiVPN.getInstance().getDatabase().isWhitelisted(ip);
        }
        return whitelistedIps.contains(ip);
    }

    public CompletableFuture<VPNResponse> checkIp(String ip) {
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
