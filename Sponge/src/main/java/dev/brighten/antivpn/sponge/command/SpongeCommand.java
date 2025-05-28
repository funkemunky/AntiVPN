package dev.brighten.antivpn.sponge.command;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.command.CommandExecutor;
import dev.brighten.antivpn.utils.StringUtil;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.ArgumentReader;

import java.util.*;
import java.util.stream.IntStream;

public class SpongeCommand implements org.spongepowered.api.command.Command.Raw {

    private final Command command;

    public SpongeCommand(Command command) {
        this.command = command;
    }

    @Override
    public CommandResult process(CommandCause sender, ArgumentReader.Mutable arguments) {

        String[] args = arguments.input().split(" ");

        CommandExecutor commandExecutor = new SpongeCommandExecutor(sender);

        val children = command.children();

        if(children.length > 0 && args.length > 0) {
            for (dev.brighten.antivpn.command.Command child : children) {
                if(child.name().equalsIgnoreCase(args[0]) || Arrays.stream(child.aliases())
                        .anyMatch(alias -> alias.equalsIgnoreCase(args[0]))) {
                    if(!sender.hasPermission("antivpn.command.*")
                            && !sender.hasPermission(child.permission())) {
                        return CommandResult.error(Component.text(StringUtil.translateAlternateColorCodes('&',
                                AntiVPN.getInstance().getMessageHandler().getString("no-permission").getMessage())));
                    }

                    commandExecutor.sendMessage(StringUtil
                                    .translateAlternateColorCodes('&',
                                            child.execute(commandExecutor,  IntStream
                                                    .range(0, args.length - 1)
                                                    .mapToObj(i -> args[i + 1]).toArray(String[]::new))));
                    return CommandResult.success();
                }
            }
        }

        commandExecutor.sendMessage(StringUtil
                .translateAlternateColorCodes('&',
                        command.execute(new SpongeCommandExecutor(sender), args)));

        command.execute(new SpongeCommandExecutor(sender), args);
        return CommandResult.success();
    }

    @Override
    public List<CommandCompletion> complete(CommandCause sender, ArgumentReader.Mutable arguments) {
        val children = command.children();
        String[] args = arguments.input().split(" ");
        if(children.length > 0 && args.length > 0) {
            for (dev.brighten.antivpn.command.Command child : children) {
                if(child.name().equalsIgnoreCase(args[0])  || Arrays.stream(child.aliases())
                        .anyMatch(alias2 -> alias2.equalsIgnoreCase(args[0]))) {
                    return child.tabComplete(new SpongeCommandExecutor(sender), "alias", IntStream
                            .range(0, args.length - 1)
                            .mapToObj(i -> args[i + 1]).toArray(String[]::new))
                            .stream()
                            .map(CommandCompletion::of)
                            .toList();
                }
            }
        }
        return command.tabComplete(new SpongeCommandExecutor(sender), "alias", args)
                .stream()
                .map(CommandCompletion::of)
                .toList();
    }

    @Override
    public boolean canExecute(CommandCause cause) {
        return cause.hasPermission(command.permission());
    }

    @Override
    public Optional<Component> shortDescription(CommandCause cause) {
        return command.description() != null ? Optional.of(Component.text(command.description())) : Optional.empty();
    }

    @Override
    public Optional<Component> extendedDescription(CommandCause cause) {
        return Optional.empty();
    }

    @Override
    public Optional<Component> help(@NonNull CommandCause cause) {
        return Optional.of(Component.text(StringUtil.translateAlternateColorCodes('&',
                command.execute(new SpongeCommandExecutor(cause), new String[0]))));
    }

    @Override
    public Component usage(CommandCause cause) {
        return command.usage() != null ? Component.text(command.usage()) : Component.empty();
    }
}
