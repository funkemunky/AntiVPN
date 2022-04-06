package dev.brighten.antivpn.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.CommandExecutor;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Optional;

@RequiredArgsConstructor
public class VelocityCommandExecutor implements CommandExecutor {

    private final CommandSource sender;

    @Override
    public void sendMessage(String message) {
        sender.sendMessage(LegacyComponentSerializer.builder().character('&').build().deserialize(message));
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public Optional<APIPlayer> getPlayer() {
        if(!isPlayer()) return Optional.empty();

        return AntiVPN.getInstance().getPlayerExecutor().getPlayer(((Player) sender).getUniqueId());
    }

    @Override
    public boolean isPlayer() {
        return sender instanceof Player;
    }
}
