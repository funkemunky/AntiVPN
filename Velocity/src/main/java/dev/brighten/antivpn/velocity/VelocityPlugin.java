package dev.brighten.antivpn.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.local.H2VPN;
import dev.brighten.antivpn.database.mongo.MongoVPN;
import dev.brighten.antivpn.database.sql.MySqlVPN;
import dev.brighten.antivpn.velocity.command.VelocityCommand;
import lombok.Getter;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.logging.Logger;

@Getter
@Plugin(id = "kaurivpn", name = "KauriVPN", version = "1.7.1", authors = {"funkemunky"})
public class VelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Metrics.Factory metricsFactory;
    private final Path configDir;
    @Nullable
    private Metrics metrics;

    public static VelocityPlugin INSTANCE;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path path, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.configDir = path;
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
            metrics = metricsFactory.make(this, 12791);

            metrics.addCustomChart(new SimplePie("database_used", this::getDatabaseType));
        }

        logger.info("Registering commands...");
        for (Command command : AntiVPN.getInstance().getCommands()) {
            server.getCommandManager().register(server.getCommandManager().metaBuilder(command.name())
                            .aliases(command.aliases()).build(), new VelocityCommand(command));
        }
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        logger.info("Disabling AntiVPN...");
        AntiVPN.getInstance().getExecutor().log("Disabling AntiVPN...");

        if (AntiVPN.getInstance().getDatabase() != null) {
            AntiVPN.getInstance().stop();
        }

        if (metrics != null) {
            metrics = null;
        }

        INSTANCE = null;
        logger.info("Disabled AntiVPN.");
    }

    private String getDatabaseType() {
        VPNDatabase database = AntiVPN.getInstance().getDatabase();

        if(database instanceof H2VPN) {
            return "H2";
        } else if(database instanceof MySqlVPN) {
            return "MySQL";
        } else if(database instanceof MongoVPN) {
            return "MongoDB";
        } else {
            return "No-Database";
        }
    }
}
