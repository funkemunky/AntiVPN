package dev.brighten.antivpn.command;

import dev.brighten.antivpn.AntiVPN;

public abstract class Command {

    public abstract String permission();

    public abstract String name();

    public abstract String[] aliases();

    public abstract String description();

    public abstract String usage();

    public abstract String parent();
    
    public abstract String execute(CommandExecutor executor, String[] args);
}
