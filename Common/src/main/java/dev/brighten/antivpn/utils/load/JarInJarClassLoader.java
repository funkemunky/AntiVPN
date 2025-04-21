/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package dev.brighten.antivpn.utils.load;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Classloader that can load a jar from within another jar file.
 *
 * <p>The "loader" jar contains the loading code & public API classes,
 * and is class-loaded by the platform.</p>
 *
 * <p>The inner "plugin" jar contains the plugin itself, and is class-loaded
 * by the loading code & this classloader.</p>
 */
public class JarInJarClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Creates a new jar-in-jar class loader.
     *
     * @param loaderClassLoader the loader plugin's classloader (setup and created by the platform)
     * @throws LoadingException if something unexpectedly bad happens
     */
    public JarInJarClassLoader(ClassLoader loaderClassLoader) throws LoadingException {
        super(new URL[]{}, loaderClassLoader);
    }

    public void addJarToClasspath(URL url) {
        addURL(url);
    }

    public void deleteJarResource() {
        URL[] urls = getURLs();
        if (urls.length == 0) {
            return;
        }

        try {
            Path path = Paths.get(urls[0].toURI());
            Files.deleteIfExists(path);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Creates a new plugin instance.
     *
     * @param bootstrapClass the name of the bootstrap plugin class
     * @param loaderPluginType the type of the loader plugin, the only parameter of the bootstrap
     *                         plugin constructor
     * @param loaderPlugin the loader plugin instance
     * @param <T> the type of the loader plugin
     * @return the instantiated bootstrap plugin
     */
    public <T> LoaderBootstrap instantiatePlugin(String bootstrapClass, Class<T> loaderPluginType, T loaderPlugin) throws LoadingException {
        Class<? extends LoaderBootstrap> plugin;
        try {
            plugin = loadClass(bootstrapClass).asSubclass(LoaderBootstrap.class);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to load bootstrap class", e);
        }

        Constructor<? extends LoaderBootstrap> constructor;
        try {
            constructor = plugin.getConstructor(loaderPluginType);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to get bootstrap constructor", e);
        }

        try {
            return constructor.newInstance(loaderPlugin);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to create bootstrap plugin instance", e);
        }
    }

}
