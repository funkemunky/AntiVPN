package dev.brighten.antivpn.command;

import java.util.List;

public abstract class Command {

    public abstract String permission();

    public abstract String name();

    public abstract String[] aliases();

    public abstract String description();

    public abstract String usage();

    public abstract String parent();

    public abstract Command[] children();
    
    public abstract String execute(CommandExecutor executor, String[] args);

    public abstract List<String> tabComplete(CommandExecutor executor, String alias, String[] args);
}
