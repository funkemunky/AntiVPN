package dev.brighten.antivpn.api;

import java.util.List;

public interface VPNConfig {

    String getLicense();

    boolean cachedResults();

    String getKickString();

    List<String> getPrefixWhitelists();

    boolean isDatabaseEnabled();

    String getDatabaseType();

    String getDatabaseName();

    String getUsername();

    String getPassword();

    String getIp();

    int getPort();

    void update();

}
