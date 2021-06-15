package dev.brighten.antivpn.api;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.VPNResponse;
import dev.brighten.antivpn.utils.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public abstract class VPNExecutor {
    public static ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    private static final Map<String, VPNResponse> responseCache = new HashMap<>();

    public abstract void registerListeners();

    public abstract void runCacheReset();

    public void resetCache() {
        responseCache.clear();
    }

    public abstract void shutdown();

    public void checkIp(String ip, boolean cachedResults, Consumer<VPNResponse> result) {
        threadExecutor.execute(() -> result.accept(responseCache.compute(ip, (key, val) -> {
            if(val == null) {
                try {
                    return AntiVPN.getVPNResponse(ip, AntiVPN.getInstance().getConfig().getLicense(), cachedResults);
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }

            return val;
        })));
    }

    public VPNResponse checkIp(String ip, boolean cachedResults) {
        return responseCache.compute(ip, (key, val) -> {
            if(val == null) {
                try {
                    return AntiVPN.getVPNResponse(ip, AntiVPN.getInstance().getConfig().getLicense(), cachedResults);
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
            }

            return val;
        });
    }
}
