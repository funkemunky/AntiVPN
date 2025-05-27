package dev.brighten.antivpn.sponge;

import com.google.inject.Inject;
import dev.brighten.antivpn.AntiVPN;
import lombok.Getter;
import org.spongepowered.api.Server;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin("kaurivpn")
@Getter
public class SpongePlugin {

    public static SpongePlugin INSTANCE;

    //Plugin init
    @Inject
    private PluginContainer plugin;

    @Inject
    private Logger logger;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;


    @Listener
    public void onServerStart(final StartedEngineEvent<Server> event) {
        INSTANCE = this;

        logger.info("Starting AntiVPN services...");
        //Start AntiVPN

        SpongeListener spongeListener = new SpongeListener();

        AntiVPN.start(spongeListener, new SpongePlayerExecutor(), configDir.toFile());
    }

    @Listener
    public void onServer(final StoppingEngineEvent<?> event) {
        AntiVPN.getInstance().getExecutor().disablePlugin();
    }

}
