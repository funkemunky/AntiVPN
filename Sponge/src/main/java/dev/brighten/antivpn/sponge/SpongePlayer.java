package dev.brighten.antivpn.sponge;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.sponge.util.StringUtil;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

public class SpongePlayer extends APIPlayer {

    private final ServerPlayer player;

    public SpongePlayer(ServerPlayer player) {
        super(player.uniqueId(), player.name(), player.connection().address().getAddress());
        this.player = player;
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(StringUtil.translateColorCodes('&', message);
    }

    @Override
    public void kickPlayer(String reason) {
        player.kick(Component.text(StringUtil.translateColorCodes('&', reason)));
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }
}
