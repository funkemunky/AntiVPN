package dev.brighten.antivpn.command;

import dev.brighten.antivpn.api.APIPlayer;

import java.util.Optional;

public interface CommandExecutor {

    void sendMessage(String message);
    boolean hasPermission(String permission);
    Optional<APIPlayer> getPlayer();
    boolean isPlayer();

}
