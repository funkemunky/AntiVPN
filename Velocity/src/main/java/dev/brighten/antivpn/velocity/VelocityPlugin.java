package dev.brighten.antivpn.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.command.Command;
import lombok.Getter;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bstats.velocity.Metrics;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.IntStream;

@Getter
@Plugin(id = "kaurivpn", name = "KauriVPN", version = "1.5.0", authors = {"funkemunky"})
public class VelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private Metrics.Factory metricsFactory;

    public static VelocityPlugin INSTANCE;

    @Inject
    @DataDirectory
    private Path configDir;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        INSTANCE = this;
        logger.info("Loading config...");

        //Loading plugin
        logger.info("Starting AntiVPN services...");
        AntiVPN.start(new VelocityListener(), new VelocityPlayerExecutor(), configDir.toFile());

        if(AntiVPN.getInstance().getVpnConfig().metrics()) {
            logger.info("Starting metrics...");
            Metrics metrics = metricsFactory.make(this, 12791);
        }

        logger.info("Registering commands...");
        for (Command command : AntiVPN.getInstance().getCommands()) {
            server.getCommandManager().register(server.getCommandManager().metaBuilder(command.name())
                            .aliases(command.aliases()).build(), (SimpleCommand) invocation -> {
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
                    });
        }
    }
}
