package dev.brighten.antivpn.api;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.VPNResponse;
import dev.brighten.antivpn.utils.json.JSONException;
import lombok.Getter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class VPNExecutor {
    public static ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    private static final Map<String, VPNResponse> responseCache = new HashMap<>();
    @Getter
    private final Set<UUID> whitelisted = Collections.synchronizedSet(new HashSet<>());

    public abstract void registerListeners();

    public abstract void runCacheReset();

    public void resetCache() {
        responseCache.clear();
    }

    public abstract void shutdown();

    public boolean isWhitelisted(UUID uuid) {
        return whitelisted.contains(uuid);
    }

    public void checkIp(String ip, boolean cachedResults, Consumer<VPNResponse> result) {
        threadExecutor.execute(() -> result.accept(responseCache.compute(ip, (key, val) -> {
            if(val == null) {
                Optional<VPNResponse> cachedRes = AntiVPN.getInstance().getDatabase().getStoredResponse(ip);

                if(cachedRes.isPresent()) return cachedRes.get();
                else {
                    try {
                        VPNResponse response =  AntiVPN
                                .getVPNResponse(ip, AntiVPN.getInstance().getConfig().getLicense(), cachedResults);

                        if(response.isSuccess()) {
                            AntiVPN.getInstance().getDatabase().cacheResponse(response);
                        } else {
                            System.out.println("Query to VPN API failed! Reason: " + response.getFailureReason());
                        }

                        return response;
                    } catch (JSONException | IOException e) {
                        System.out.println("Query to VPN API failed! Reason: Java Exception");
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
                        VPNResponse response =  AntiVPN
                                .getVPNResponse(ip, AntiVPN.getInstance().getConfig().getLicense(), cachedResults);

                        if(response.isSuccess()) {
                            threadExecutor.execute(() -> AntiVPN.getInstance().getDatabase().cacheResponse(response));
                        } else {
                            System.out.println("Query to VPN API failed! Reason: " + response.getFailureReason());
                        }

                        return response;
                    } catch (JSONException | IOException e) {
                        System.out.println("Query to VPN API failed! Reason: Java Exception");
                        e.printStackTrace();
                    }
                }
            }

            return val;
        });
    }
}
