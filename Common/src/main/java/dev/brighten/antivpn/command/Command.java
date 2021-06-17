package dev.brighten.antivpn.command;

import dev.brighten.antivpn.AntiVPN;

public abstract class Command {

    public Command() {
        for (Command child : children()) {
            AntiVPN.getInstance().getCommands().add(child);
        }
    }

    public abstract String permission();

    public abstract String name();

    public abstract String[] aliases();

    public abstract String description();

    public abstract String usage();

    public abstract String parent();

    public abstract Command[] children();
    
    public abstract String execute(CommandExecutor executor, String[] args);
}
