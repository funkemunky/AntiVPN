package dev.brighten.antivpn.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import dev.brighten.antivpn.command.Command;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

public class VelocityCommand implements SimpleCommand {

    private final Command command;

    public VelocityCommand(Command command) {
        this.command = command;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        if(!invocation.source().hasPermission("antivpn.command.*")
                && !invocation.source().hasPermission(command.permission())) {
            invocation.source().sendMessage(Component.text("No permission").toBuilder()
                    .color(TextColor.color(255,0,0)).build());
            return;
        }

        val children = command.children();

        String[] args = invocation.arguments();
        if(children.length > 0 && args.length > 0) {
            for (Command child : children) {
                if(child.name().equalsIgnoreCase(args[0]) || Arrays.stream(child.aliases())
                        .anyMatch(alias -> alias.equalsIgnoreCase(args[0]))) {
                    if(!sender.hasPermission("antivpn.command.*")
                            && !sender.hasPermission(child.permission())) {
                        invocation.source().sendMessage(Component.text("No permission")
                                .toBuilder().color(TextColor.color(255,0,0)).build());
                        return;
                    }
                    sender.sendMessage(LegacyComponentSerializer.builder().character('&').build()
                            .deserialize(child.execute(new VelocityCommandExecutor(sender),  IntStream
                                    .range(0, args.length - 1)
                                    .mapToObj(i -> args[i + 1]).toArray(String[]::new))));
                    return;
                }
            }
        }

        sender.sendMessage(LegacyComponentSerializer.builder().character('&').build()
                .deserialize(command.execute(new VelocityCommandExecutor(sender), args)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        final CommandSource sender = invocation.source();
        final String[] args = invocation.arguments();

        val children = command.children();

        if(children.length > 0 && args.length > 0) {
            for (dev.brighten.antivpn.command.Command child : children) {
                if(child.name().equalsIgnoreCase(args[0])  || Arrays.stream(child.aliases())
                        .anyMatch(alias2 -> alias2.equalsIgnoreCase(args[0]))) {
                    return child.tabComplete(new VelocityCommandExecutor(sender), "alias", IntStream
                            .range(0, args.length - 1)
                            .mapToObj(i -> args[i + 1]).toArray(String[]::new));
                }
            }
        }
        return command.tabComplete(new VelocityCommandExecutor(sender), "alias", args);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return  CompletableFuture.supplyAsync(() -> this.suggest(invocation));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return SimpleCommand.super.hasPermission(invocation);
    }
}
