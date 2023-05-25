package dev.brighten.antivpn.sponge;

import com.google.inject.Inject;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.util.logging.Logger;

@Plugin("kaurivpn")
public class SpongePlugin {

    public static SpongePlugin INSTANCE;
    //Plugin init

    @Inject
    private Logger logger;

    @Listener
    public void onServerStart(final StartedEngineEvent<Server> event) {
        INSTANCE = this;

        logger.info("Starting AntiVPN services...");
        //Start AntiVPN
    }

}
