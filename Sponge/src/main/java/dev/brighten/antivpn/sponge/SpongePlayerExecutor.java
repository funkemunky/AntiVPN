package dev.brighten.antivpn.sponge;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.PlayerExecutor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SpongePlayerExecutor implements PlayerExecutor {

    private final Cache<UUID, SpongePlayer> playerCache = Caffeine.newBuilder().maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    @Override
    public Optional<APIPlayer> getPlayer(String name) {
        Optional<ServerPlayer> serverPlayer = Sponge.server().player(name);

        return serverPlayer.map(SpongePlayer::new);
    }

    @Override
    public Optional<APIPlayer> getPlayer(UUID uuid) {
        SpongePlayer cachedPlayer = playerCache.getIfPresent(uuid);

        if(cachedPlayer != null) {
            return Optional.of(cachedPlayer);
        }

        Optional<ServerPlayer> serverPlayer = Sponge.server().player(uuid);

        Optional<APIPlayer> player = serverPlayer.map(SpongePlayer::new);

        player.ifPresent(value -> playerCache.put(uuid, (SpongePlayer) value));

        return player;
    }

    @Override
    public void unloadPlayer(UUID uuid) {
        playerCache.invalidate(uuid);
    }

    @Override
    public List<APIPlayer> getOnlinePlayers() {
        return Sponge.server().onlinePlayers()
                .stream()
                .map(pl -> {
                    SpongePlayer cachedPlayer = playerCache.getIfPresent(pl.uniqueId());

                    if(cachedPlayer != null) {
                        return cachedPlayer;
                    }

                    SpongePlayer player = new SpongePlayer(pl);
                    playerCache.put(pl.uniqueId(), player);

                    return (APIPlayer) player;
                })
                .toList();
    }
}
