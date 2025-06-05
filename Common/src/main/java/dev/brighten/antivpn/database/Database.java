package dev.brighten.antivpn.database;

import dev.brighten.antivpn.web.objects.VPNResponse;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings({"unused"})
public interface Database {
    Optional<VPNResponse> getStoredResponse(String ip);

    void cacheResponse(VPNResponse toCache);

    void deleteResponse(String ip);

    void clearResponses();

    void clearOutdatedResponses();

    boolean isWhitelisted(UUID uuid);

    boolean isWhitelisted(String ip);

    void addWhitelist(UUID uuid);

    void addWhitelist(String cidr);

    void removeWhitelist(UUID uuid);

    void removeWhitelist(String cidr);

    boolean getAlertsState(UUID uuid);

    void updateAlertsState(UUID uuid, boolean state);

    void init();

    void shutdown();
}
