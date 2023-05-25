package dev.brighten.antivpn.sponge;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.PlayerExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SpongePlayerExecutor implements PlayerExecutor {
    @Override
    public Optional<APIPlayer> getPlayer(String name) {

        return Optional.empty();
    }

    @Override
    public Optional<APIPlayer> getPlayer(UUID uuid) {
        return Optional.empty();
    }

    @Override
    public void unloadPlayer(UUID uuid) {

    }

    @Override
    public List<APIPlayer> getOnlinePlayers() {
        return null;
    }
}
