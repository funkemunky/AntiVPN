package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.PlayerExecutor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class BukkitPlayerExecutor implements PlayerExecutor {
    @Override
    public Optional<APIPlayer> getPlayer(String name) {
        final Player player = Bukkit.getPlayer(name);

        if(player == null) {
            return Optional.empty();
        }

        return Optional.of(new BukkitPlayer(player));
    }

    @Override
    public Optional<APIPlayer> getPlayer(UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);

        if(player == null) {
            return Optional.empty();
        }

        return Optional.of(new BukkitPlayer(player));
    }

    @Override
    public List<APIPlayer> getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().stream().map(BukkitPlayer::new).collect(Collectors.toList());
    }
}
