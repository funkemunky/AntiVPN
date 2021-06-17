package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.command.CommandExecutor;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

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
}
