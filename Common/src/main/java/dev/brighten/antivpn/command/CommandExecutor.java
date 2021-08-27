package dev.brighten.antivpn.command;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.api.PlayerExecutor;

import java.util.Optional;

public interface CommandExecutor {

    void sendMessage(String message);
    boolean hasPermission(String permission);
    Optional<APIPlayer> getPlayer();
    boolean isPlayer();

}
