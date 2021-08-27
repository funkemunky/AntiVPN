package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.CommandExecutor;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Optional;

@RequiredArgsConstructor
public class BungeeCommandExecutor implements CommandExecutor {

    private final CommandSender executor;

    @Override
    public void sendMessage(String message) {
        executor.sendMessage(TextComponent.fromLegacyText(ChatColor
                .translateAlternateColorCodes('&', message)));
    }

    @Override
    public boolean hasPermission(String permission) {
        return executor.hasPermission(permission);
    }

    @Override
    public Optional<APIPlayer> getPlayer() {
        if(!isPlayer()) return Optional.empty();

        return AntiVPN.getInstance().getPlayerExecutor().getPlayer(((ProxiedPlayer)executor).getUniqueId());
    }

    @Override
    public boolean isPlayer() {
        return executor instanceof ProxiedPlayer;
    }
}
