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

package dev.brighten.antivpn.bungee;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.bungee.command.BungeeCommand;
import dev.brighten.antivpn.command.Command;
import dev.brighten.antivpn.database.VPNDatabase;
import dev.brighten.antivpn.database.local.H2VPN;
import dev.brighten.antivpn.database.mongo.MongoVPN;
import dev.brighten.antivpn.database.sql.MySqlVPN;
import dev.brighten.antivpn.loader.LoaderBootstrap;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class BungeePlugin implements LoaderBootstrap {

    public static BungeePlugin pluginInstance;
    
    @Getter
    private File dataFolder;

    @Getter
    private final Plugin plugin;

    public BungeePlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onLoad(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public void onEnable() {
        pluginInstance = this;

        //Setting up config
        ProxyServer.getInstance().getLogger().info("Loading config...");


        //Loading plugin
        ProxyServer.getInstance().getLogger().info("Starting AntiVPN services...");
        AntiVPN.start(new BungeeListener(), new BungeePlayerExecutor(), getDataFolder());

        if(AntiVPN.getInstance().getVpnConfig().metrics()) {
            ProxyServer.getInstance().getLogger().info("Starting bStats metrics...");
            Metrics metrics = new Metrics(getPlugin(), 12616);
            metrics.addCustomChart(new SimplePie("database_used", this::getDatabaseType));
            ProxyServer.getInstance().getScheduler().schedule(getPlugin(),
                    () -> AntiVPN.getInstance().checked = AntiVPN.getInstance().detections = 0,
                    10, 10, TimeUnit.MINUTES);
        }

        for (Command command : AntiVPN.getInstance().getCommands()) {
            ProxyServer.getInstance().getPluginManager().registerCommand(getPlugin(), new BungeeCommand(command));
        }
    }

    @Override
    public void onDisable() {
        AntiVPN.getInstance().stop();
    }

    private String getDatabaseType() {
        VPNDatabase database = AntiVPN.getInstance().getDatabase();
        if(database instanceof MySqlVPN) {
            return "MySQL";
        } else if(database instanceof H2VPN) {
            return "H2";
        } else if(database instanceof MongoVPN) {
            return "MongoDB";
        } else {
            return "No-Database";
        }
    }

    public ProxyServer getProxy() {
        return ProxyServer.getInstance();
    }
}
