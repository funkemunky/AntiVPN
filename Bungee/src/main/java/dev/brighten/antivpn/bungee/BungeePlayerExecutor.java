package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.PlayerExecutor;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class BungeePlayerExecutor implements PlayerExecutor {

    @Override
    public Optional<APIPlayer> getPlayer(String name) {
        ProxiedPlayer player = BungeeCord.getInstance().getPlayer(name);

        if(player == null) return Optional.empty();

        return Optional.of(new BungeePlayer(player));
    }

    @Override
    public Optional<APIPlayer> getPlayer(UUID uuid) {
        ProxiedPlayer player = BungeeCord.getInstance().getPlayer(uuid);

        if(player == null) return Optional.empty();

        return Optional.of(new BungeePlayer(player));
    }

    @Override
    public List<APIPlayer> getOnlinePlayers() {
        return BungeeCord.getInstance().getPlayers().stream().map(BungeePlayer::new).collect(Collectors.toList());
    }
}
