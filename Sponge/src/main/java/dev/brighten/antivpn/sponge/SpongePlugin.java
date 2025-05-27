package dev.brighten.antivpn.sponge;

import com.google.inject.Inject;
import dev.brighten.antivpn.AntiVPN;
import lombok.Getter;
import org.spongepowered.api.Server;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.*;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.nio.file.Path;

@Plugin("kaurivpn")
@Getter
public class SpongePlugin {

    //Plugin init
    private final PluginContainer container;
    private final Logger logger;

    @Inject
    SpongePlugin(final PluginContainer container, final Logger logger) {
        this.container = container;
        this.logger = logger;
    }


    @Listener
    public void onServerStart(final ConstructPluginEvent event) {
        //Start AntiVPN

        ConfigManager configManager = Sponge.game().configManager();
        SpongeListener spongeListener = new SpongeListener();

        var path = configManager.sharedConfig(container).directory();

        logger.info("Fucking path: " + path);

        AntiVPN.start(spongeListener, new SpongePlayerExecutor(), path.toFile());
    }

    @Listener
    public void onServer(final StoppingEngineEvent<Server> event) {
        AntiVPN.getInstance().getExecutor().disablePlugin();
    }

    public static SpongePlugin getInstance() {
        return (SpongePlugin) Sponge.pluginManager().plugin("kaurivpn").get();
    }

}
