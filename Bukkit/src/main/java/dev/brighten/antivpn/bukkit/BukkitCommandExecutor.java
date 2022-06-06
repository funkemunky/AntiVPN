package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.command.CommandExecutor;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

@RequiredArgsConstructor
public class BukkitCommandExecutor implements CommandExecutor {

    private final CommandSender sender;

    @Override
    public void sendMessage(String message, Object... objects) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                String.format(message, objects)));
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    public Optional<APIPlayer> getPlayer() {
        if(!isPlayer()) return Optional.empty();

        return AntiVPN.getInstance().getPlayerExecutor().getPlayer(((Player)sender).getUniqueId());
    }

    @Override
    public boolean isPlayer() {
        return sender instanceof Player;
    }
}
