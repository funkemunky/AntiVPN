package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.PlayerExecutor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class BungeePlayerExecutor implements PlayerExecutor {

    private final Map<UUID, BungeePlayer> cachedPlayers = new HashMap<>();

    @Override
    public Optional<APIPlayer> getPlayer(String name) {
        ProxiedPlayer player = BungeePlugin.pluginInstance.getProxy().getPlayer(name);

        if(player == null) return Optional.empty();

        return Optional.of(cachedPlayers.computeIfAbsent(player.getUniqueId(), key -> new BungeePlayer(player)));
    }

    @Override
    public Optional<APIPlayer> getPlayer(UUID uuid) {
        ProxiedPlayer player = BungeePlugin.pluginInstance.getProxy().getPlayer(uuid);

        if(player == null) return Optional.empty();

        return Optional.of(cachedPlayers.computeIfAbsent(uuid, key -> new BungeePlayer(player)));
    }

    @Override
    public void unloadPlayer(UUID uuid) {
        this.cachedPlayers.remove(uuid);
    }

    @Override
    public List<APIPlayer> getOnlinePlayers() {
        return BungeePlugin.pluginInstance.getProxy().getPlayers().stream()
                .map(pl -> cachedPlayers.computeIfAbsent(pl.getUniqueId(), key -> new BungeePlayer(pl)))
                .collect(Collectors.toList());
    }
}
