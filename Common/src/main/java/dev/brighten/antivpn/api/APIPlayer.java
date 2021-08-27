package dev.brighten.antivpn.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;
import java.util.UUID;

@Getter
@RequiredArgsConstructor
public abstract class APIPlayer {
    private final UUID uuid;
    private final String name;
    private final InetAddress ip;
    @Setter
    private boolean alertsEnabled;

    public abstract void sendMessage(String message);

    public abstract void kickPlayer(String reason);

    public abstract boolean hasPermission(String permission);
}
