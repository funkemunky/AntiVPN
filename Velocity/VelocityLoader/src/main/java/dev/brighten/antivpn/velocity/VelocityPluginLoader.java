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

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.brighten.antivpn.loader.JarInJarClassLoader;
import dev.brighten.antivpn.loader.LoaderBootstrap;
import org.bstats.velocity.Metrics;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Plugin(id = "kaurivpn", name = "KauriVPN", version = "1.7.1", authors = {"funkemunky"})
public class VelocityPluginLoader {

    private static final String JAR_NAME = "antivpn-velocity.jarinjar";
    private static final String SOURCE_NAME = "antivpn-source.jarinjar";
    private static final String BOOTSTRAP_CLASS = "dev.brighten.antivpn.velocity.VelocityPlugin";

    private final LoaderBootstrap plugin;
    private final ClassLoader pluginClassLoader;

    @Inject
    public VelocityPluginLoader(ProxyServer server, Logger logger, @DataDirectory Path path, Metrics.Factory metricsFactory) {
        Map<Class<?>, Object> instances = new HashMap<>();
        instances.put(ProxyServer.class, server);
        instances.put(Logger.class, logger);
        instances.put(Path.class, path);
        instances.put(String.class, metricsFactory);
        instances.put(LoaderBootstrap.class, this);
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), JAR_NAME, SOURCE_NAME);
        this.pluginClassLoader = loader;
        this.plugin = loader.instantiatePlugin(BOOTSTRAP_CLASS, Map.class, instances);
        runWithPluginClassLoader(() -> plugin.onLoad(path.toFile()));
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        runWithPluginClassLoader(plugin::onEnable);
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        runWithPluginClassLoader(plugin::onDisable);
    }

    private void runWithPluginClassLoader(Runnable action) {
        Thread thread = Thread.currentThread();
        ClassLoader previousClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(pluginClassLoader);
        try {
            action.run();
        } finally {
            thread.setContextClassLoader(previousClassLoader);
        }
    }

}
