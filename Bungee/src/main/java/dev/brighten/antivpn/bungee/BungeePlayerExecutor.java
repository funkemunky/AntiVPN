package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.PlayerExecutor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class BungeePlayerExecutor implements PlayerExecutor {

    private final Map<ProxiedPlayer, BungeePlayer> cachedPlayers = new WeakHashMap<>();

    @Override
    public Optional<APIPlayer> getPlayer(String name) {
        ProxiedPlayer player = BungeeCord.getInstance().getPlayer(name);

        if(player == null) return Optional.empty();

        return Optional.of(cachedPlayers.computeIfAbsent(player, BungeePlayer::new));
    }

    @Override
    public Optional<APIPlayer> getPlayer(UUID uuid) {
        ProxiedPlayer player = BungeeCord.getInstance().getPlayer(uuid);

        if(player == null) return Optional.empty();

        return Optional.of(cachedPlayers.computeIfAbsent(player, BungeePlayer::new));
    }

    @Override
    public List<APIPlayer> getOnlinePlayers() {
        return BungeeCord.getInstance().getPlayers().stream()
                .map(pl -> cachedPlayers.computeIfAbsent(pl, BungeePlayer::new))
                .collect(Collectors.toList());
    }
}
