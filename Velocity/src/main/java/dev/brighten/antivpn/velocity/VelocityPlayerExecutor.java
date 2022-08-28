package dev.brighten.antivpn.velocity;

import com.velocitypowered.api.proxy.Player;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.PlayerExecutor;

import java.util.*;
import java.util.stream.Collectors;

public class VelocityPlayerExecutor implements PlayerExecutor {

    private final Map<UUID, VelocityPlayer> cachedPlayers = new HashMap<>();

    @Override
    public Optional<APIPlayer> getPlayer(String name) {
        Optional<Player> player = VelocityPlugin.INSTANCE.getServer().getPlayer(name);

        return player.map(value -> cachedPlayers.computeIfAbsent(value.getUniqueId(),
                key -> new VelocityPlayer(value)));

    }

    @Override
    public Optional<APIPlayer> getPlayer(UUID uuid) {
        Optional<Player> player = VelocityPlugin.INSTANCE.getServer().getPlayer(uuid);

        return player.map(value -> cachedPlayers.computeIfAbsent(value.getUniqueId(),
                key -> new VelocityPlayer(value)));
    }

    @Override
    public void unloadPlayer(UUID uuid) {
        cachedPlayers.remove(uuid);
    }

    @Override
    public List<APIPlayer> getOnlinePlayers() {
        return VelocityPlugin.INSTANCE.getServer().getAllPlayers().stream()
                .map(pl -> cachedPlayers.computeIfAbsent(pl.getUniqueId(), key -> new VelocityPlayer(pl)))
                .collect(Collectors.toList());
    }
}
