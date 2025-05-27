package dev.brighten.antivpn.api;

import java.net.InetAddress;
import java.util.UUID;

public class OfflinePlayer extends APIPlayer {

    public OfflinePlayer(UUID uuid, String name, InetAddress ip) {
        super(uuid, name, ip);
    }

    @Override
    public void sendMessage(String message) {

    }

    @Override
    public void kickPlayer(String reason) {

    }

    @Override
    public boolean hasPermission(String permission) {
        return false;
    }
}
