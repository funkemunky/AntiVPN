package dev.brighten.antivpn.api;

public interface VPNConfig {

    String getLicense();

    boolean cachedResults();

    String getKickString();

    void update();

}
