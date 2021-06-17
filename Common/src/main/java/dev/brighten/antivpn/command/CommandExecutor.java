package dev.brighten.antivpn.command;

public interface CommandExecutor {

    void sendMessage(String message);
    boolean hasPermission(String permission);

}
