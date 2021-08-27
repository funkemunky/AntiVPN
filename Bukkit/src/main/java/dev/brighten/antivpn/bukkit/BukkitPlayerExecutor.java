package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.PlayerExecutor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class BukkitPlayerExecutor implements PlayerExecutor {

    private final Map<Player, BukkitPlayer> cachedPlayers = new WeakHashMap<>();

    @Override
    public Optional<APIPlayer> getPlayer(String name) {
        final Player player = Bukkit.getPlayer(name);

        if(player == null) {
            return Optional.empty();
        }

        return Optional.of(cachedPlayers.computeIfAbsent(player, BukkitPlayer::new));
    }

    @Override
    public Optional<APIPlayer> getPlayer(UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);

        if(player == null) {
            return Optional.empty();
        }

        return Optional.of(cachedPlayers.computeIfAbsent(player, BukkitPlayer::new));
    }


    @Override
    public List<APIPlayer> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream()
                .map(pl -> cachedPlayers.computeIfAbsent(pl, BukkitPlayer::new))
                .collect(Collectors.toList());
    }

}
