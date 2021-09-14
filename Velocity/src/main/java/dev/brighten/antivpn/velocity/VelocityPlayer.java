package dev.brighten.antivpn.velocity;

import com.velocitypowered.api.proxy.Player;
import dev.brighten.antivpn.api.APIPlayer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class VelocityPlayer extends APIPlayer {

    private final Player player;
    public VelocityPlayer(Player player) {
        super(player.getUniqueId(), player.getUsername(), player.getRemoteAddress().getAddress());

        this.player = player;
    }


    @Override
    public void sendMessage(String message) {
        player.sendMessage(LegacyComponentSerializer.builder().character('&').build().deserialize(message));
    }

    @Override
    public void kickPlayer(String reason) {
        player.disconnect(LegacyComponentSerializer.builder().character('&').build().deserialize(reason));
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }
}
