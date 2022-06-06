package dev.brighten.antivpn.bungee.command;

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

    private final CommandSender sender;

    @Override
    public void sendMessage(String message, Object... objects) {
        sender.sendMessage(TextComponent.fromLegacyText(ChatColor
                .translateAlternateColorCodes('&', String.format(message, objects))));
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public Optional<APIPlayer> getPlayer() {
        if(!isPlayer()) return Optional.empty();

        return AntiVPN.getInstance().getPlayerExecutor().getPlayer(((ProxiedPlayer) sender).getUniqueId());
    }

    @Override
    public boolean isPlayer() {
        return sender instanceof ProxiedPlayer;
    }
}
