package dev.brighten.antivpn.api;

import java.util.List;

public interface VPNConfig {

    String getLicense();

    boolean cachedResults();

    String getKickString();

    List<String> getPrefixWhitelists();

    void update();

}
