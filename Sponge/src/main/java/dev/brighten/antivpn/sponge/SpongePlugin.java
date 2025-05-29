package dev.brighten.antivpn.sponge;

import com.google.inject.Inject;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.sponge.command.SpongeCommand;
import lombok.Getter;
import org.spongepowered.api.Server;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.*;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("kaurivpn")
@Getter
public class SpongePlugin {

    //Plugin init
    @Inject
    private PluginContainer container;
    @Inject
    private Logger logger;
    @Getter
    private static SpongePlugin instance;

    @Listener
    public void onConstruct(final ConstructPluginEvent event) {
        instance = this;

        ConfigManager configManager = Sponge.configManager();
        SpongeListener spongeListener = new SpongeListener();

        var path = configManager.sharedConfig(container).directory();

        logger.info("Fucking path: " + path);

        AntiVPN.start(spongeListener, new SpongePlayerExecutor(), path.toFile());
    }

    @Listener
    public void onServer(final StoppingEngineEvent<Server> event) {
        AntiVPN.getInstance().getExecutor().disablePlugin();
    }

    @Listener
    public void onRegisterRawCommands(final RegisterCommandEvent<Command.Raw> event){
        if(AntiVPN.getInstance() == null) {
            for(int i = 0 ; i < 5 ; i++) System.out.println("FUCKING NULL");
            return;
        }
        AntiVPN.getInstance().getExecutor().log("Registering commands...");
        for (dev.brighten.antivpn.command.Command command : AntiVPN.getInstance().getCommands()) {
            AntiVPN.getInstance().getExecutor().log("Registering command %s...", command.name());
            event.register(this.container, new SpongeCommand(command), command.name(), command.aliases());
        }
    }
}
