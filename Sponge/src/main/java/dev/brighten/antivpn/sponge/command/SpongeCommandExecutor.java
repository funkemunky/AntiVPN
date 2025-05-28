package dev.brighten.antivpn.sponge.command;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.CommandExecutor;
import dev.brighten.antivpn.sponge.util.StringUtil;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Optional;

@RequiredArgsConstructor
public class SpongeCommandExecutor implements CommandExecutor {

    private final CommandCause cause;

    @Override
    public void sendMessage(String message, Object... objects) {
        cause.sendMessage(Component.text(StringUtil.translateColorCodes('&',
                String.format(message, objects))));
    }

    @Override
    public boolean hasPermission(String permission) {
        return cause.hasPermission(permission);
    }

    @Override
    public Optional<APIPlayer> getPlayer() {
        if(cause.subject() instanceof ServerPlayer serverPlayer) {
            return AntiVPN.getInstance().getPlayerExecutor().getPlayer(serverPlayer.uniqueId());
        }
        return Optional.empty();
    }

    @Override
    public boolean isPlayer() {
        return cause.subject() instanceof ServerPlayer;
    }
}
