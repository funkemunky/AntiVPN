package dev.brighten.antivpn.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerExecutor {

    Optional<APIPlayer> getPlayer(String name);

    Optional<APIPlayer> getPlayer(UUID uuid);

    void unloadPlayer(UUID uuid);

    List<APIPlayer> getOnlinePlayers();
}
