package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.api.APIPlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class BungeePlayer extends APIPlayer {

    private final ProxiedPlayer player;
    public BungeePlayer(ProxiedPlayer player) {
        super(player.getUniqueId(), player.getName(), player.getAddress().getAddress());

        this.player = player;
    }


    @Override
    public void sendMessage(String message) {
        player.sendMessage(TextComponent.fromLegacyText(ChatColor
                .translateAlternateColorCodes('&', message)));
    }

    @Override
    public void kickPlayer(String reason) {
        player.disconnect(TextComponent.fromLegacyText(ChatColor
                .translateAlternateColorCodes('&', reason)));
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }
}
