/*
 * Copyright 2026 Dawson Hessler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.brighten.antivpn.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.local.H2VPN;
import dev.brighten.antivpn.database.mongo.MongoVPN;
import dev.brighten.antivpn.database.sql.MySqlVPN;
import dev.brighten.antivpn.loader.LoaderBootstrap;
import dev.brighten.antivpn.velocity.command.VelocityCommand;
import lombok.Getter;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

@Getter
public class VelocityPlugin implements LoaderBootstrap {

    private final ProxyServer server;
    private final Logger logger;
    private final Metrics.Factory metricsFactory;
    private File dataFolder;

    @Nullable
    private Metrics metrics;


    public static VelocityPlugin INSTANCE;

    private final Object pluginInstance;

    public VelocityPlugin(Map<Class<?>, Object> objectsMap) {
        this.server = (ProxyServer) objectsMap.get(ProxyServer.class);
        this.logger = (Logger) objectsMap.get(Logger.class);
        this.metricsFactory = (Metrics.Factory) objectsMap.get(String.class);
        this.pluginInstance = objectsMap.get(LoaderBootstrap.class);
    }

    private String getDatabaseType() {
        VPNDatabase database = AntiVPN.getInstance().getDatabase();
        if(database instanceof MySqlVPN) {
            return "MySQL";
        } else if(database instanceof H2VPN) {
            return "H2";
        }  else if(database instanceof MongoVPN) {
            return "MongoDB";
        } else {
            return "No-Database";
        }
    }

    @Override
    public void onLoad(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        logger.info("Loading config...");

        //Loading plugin
        logger.info("Starting AntiVPN services...");
        AntiVPN.start(new VelocityListener(), new VelocityPlayerExecutor(), dataFolder);


        if(AntiVPN.getInstance().getVpnConfig().metrics()) {
            logger.info("Starting metrics...");
            metrics = metricsFactory.make(pluginInstance, 12791);

            metrics.addCustomChart(new SimplePie("database_used", this::getDatabaseType));
        }

        logger.info("Registering commands...");
        for (Command command : AntiVPN.getInstance().getCommands()) {
            server.getCommandManager().register(server.getCommandManager().metaBuilder(command.name())
                    .aliases(command.aliases()).build(), new VelocityCommand(command));
        }
    }

    @Override
    public void onDisable() {
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
}
