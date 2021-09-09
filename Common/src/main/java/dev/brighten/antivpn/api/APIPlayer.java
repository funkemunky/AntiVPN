package dev.brighten.antivpn.api;

import dev.brighten.antivpn.AntiVPN;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;
import java.util.UUID;

@Getter
public abstract class APIPlayer {
    private final UUID uuid;
    private final String name;
    private final InetAddress ip;
    private boolean alertsEnabled;

    public APIPlayer(UUID uuid, String name, InetAddress ip) {
        this.uuid = uuid;
        this.name = name;
        this.ip = ip;

        AntiVPN.getInstance().getDatabase().alertsState(uuid, enabled -> {
            if(enabled) {
                alertsEnabled = enabled; //This can be within the if statement. It's not dirty, it lowers field writes.
                sendMessage("");
            }
        });
    }

    public abstract void sendMessage(String message);

    public abstract void kickPlayer(String reason);

    public abstract boolean hasPermission(String permission);

    public void setAlertsEnabled(boolean alertsEnabled) {
        this.alertsEnabled = alertsEnabled;
        //Updating into database so its synced across servers and saved on logout.
        AntiVPN.getInstance().getDatabase().updateAlertsState(uuid, alertsEnabled);
    }
}
