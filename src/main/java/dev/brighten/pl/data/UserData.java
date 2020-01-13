package dev.brighten.pl.data;

import dev.brighten.pl.vpn.VPNResponse;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class UserData {
    public final static Map<UUID, UserData> dataMap = Collections.synchronizedMap(new HashMap<>());

    public final UUID uuid;
    private Player player;
    public VPNResponse response;
    public boolean hasAlerts;

    public static UserData getData(UUID uuid) {
        return dataMap.computeIfAbsent(uuid, key -> {
            if(Bukkit.getPlayer(uuid) != null) {
                UserData data = new UserData(uuid);
                dataMap.put(key, data);
                return data;
            }
            return null;
        });
    }

    public Player getPlayer() {
        if(player == null) {
            return player = Bukkit.getPlayer(uuid);
        }
        return player;
    }
}
