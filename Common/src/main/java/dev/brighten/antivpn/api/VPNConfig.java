package dev.brighten.antivpn.api;

import java.util.List;

public interface VPNConfig {

    String getLicense();

    boolean cachedResults();

    String getKickString();

    String alertMessage();

    boolean alertToStaff();

    boolean runCommands();

    List<String> commands();

    boolean kickPlayersOnDetect();

    List<String> getPrefixWhitelists();

    boolean isDatabaseEnabled();

    boolean useDatabaseCreds();

    String mongoDatabaseURL();

    String getDatabaseType();

    String getDatabaseName();

    String getUsername();

    String getPassword();

    String getIp();

    int getPort();

    boolean metrics();

    void update();

}
