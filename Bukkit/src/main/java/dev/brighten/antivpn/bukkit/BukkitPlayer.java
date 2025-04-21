package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.api.APIPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BukkitPlayer extends APIPlayer {

    private final Player player;
    public BukkitPlayer(Player player) {
        super(player.getUniqueId(), player.getName(), player.getAddress().getAddress());

        this.player = player;
    }

    @Override
    public void sendMessage(String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void kickPlayer(String reason) {
        if(!Bukkit.isPrimaryThread()) {
            new BukkitRunnable() {
                public void run() {
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&', reason));
                }
            }.runTask(BukkitBootstrap.pluginInstance);
        } else player.kickPlayer(ChatColor.translateAlternateColorCodes('&', reason));
    }

    @Override
    public boolean hasPermission(String permission) {
        return player.hasPermission(permission);
    }
}
