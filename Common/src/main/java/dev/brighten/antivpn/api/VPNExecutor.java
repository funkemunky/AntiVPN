package dev.brighten.antivpn.api;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.EvictingMap;
import dev.brighten.antivpn.web.objects.VPNResponse;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.web.FunkemunkyAPI;
import lombok.Getter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;

public abstract class VPNExecutor {
    public static ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    public static final Map<String, VPNResponse> responseCache = new EvictingMap<>(5000);
    @Getter
    private final Set<UUID> whitelisted = Collections.synchronizedSet(new HashSet<>());
    @Getter
    private final Set<String> whitelistedIps = Collections.synchronizedSet(new HashSet<>());

    public abstract void registerListeners();

    public abstract void runCacheReset();

    public void resetCache() {
        responseCache.clear();
    }

    public abstract void shutdown();

    public abstract void log(Level level, String log, Object... objects);

    public abstract void log(String log, Object... objects);

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

    public void checkIp(String ip, boolean cachedResults, Consumer<VPNResponse> result) {
        threadExecutor.execute(() -> result.accept(responseCache.compute(ip, (key, val) -> {
            if(val == null) {
                Optional<VPNResponse> cachedRes = AntiVPN.getInstance().getDatabase().getStoredResponse(ip);

                if(cachedRes.isPresent()) return cachedRes.get();
                else {
                    try {
                        VPNResponse response =  FunkemunkyAPI
                                .getVPNResponse(ip, AntiVPN.getInstance().getVpnConfig().getLicense(), cachedResults);

                        if(response.isSuccess()) {
                            AntiVPN.getInstance().getDatabase().cacheResponse(response);
                        } else {
                            log("Query to VPN API failed! Reason: " + response.getFailureReason());
                        }

                        return response;
                    } catch (JSONException | IOException e) {
                        log("Query to VPN API failed! Reason: Java Exception");
                        e.printStackTrace();
                    }
                }
            }

            return val;
        })));
    }

    public VPNResponse checkIp(String ip, boolean cachedResults) {
        return responseCache.compute(ip, (key, val) -> {
            if(val == null) {
                Optional<VPNResponse> cachedRes = AntiVPN.getInstance().getDatabase().getStoredResponse(ip);

                if(cachedRes.isPresent()) return cachedRes.get();
                else {
                    try {
                        VPNResponse response =  FunkemunkyAPI
                                .getVPNResponse(ip, AntiVPN.getInstance().getVpnConfig().getLicense(), cachedResults);

                        if(response.isSuccess()) {
                            threadExecutor.execute(() -> AntiVPN.getInstance().getDatabase().cacheResponse(response));
                        } else {
                            log("Query to VPN API failed! Reason: " + response.getFailureReason());
                        }

                        return response;
                    } catch (JSONException | IOException e) {
                        log("Query to VPN API failed! Reason: Java Exception");
                        e.printStackTrace();
                    }
                }
            }

            return val;
        });
    }
}
