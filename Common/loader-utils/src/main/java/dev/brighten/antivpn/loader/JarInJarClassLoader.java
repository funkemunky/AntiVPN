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

package dev.brighten.antivpn.loader;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Classloader that can load a jar from within another jar file.
 *
 * <p>The "loader" jar contains the loading code & public API classes
 * and is class-loaded by the platform.</p>
 *
 * <p>The inner "plugin" jar contains the plugin itself and is class-loaded
 * by the loading code & this classloader.</p>
 */
public class JarInJarClassLoader extends URLClassLoader {
    private static final String[] PARENT_FIRST_PACKAGES = {
            "java.",
            "javax.",
            "jdk.",
            "sun.",
            "com.sun.",
            "org.w3c.",
            "org.xml.",
            "org.slf4j.",
            "com.google.inject.",
            "javax.inject.",
            "com.velocitypowered.",
            "org.bukkit.",
            "net.md_5.bungee.",
            "org.spongepowered.",
            "org.bstats.",
            "dev.brighten.antivpn.velocity.org.bstats.",
            "dev.brighten.antivpn.loader."
    };

    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Creates a new jar-in-jar class loader.
     *
     * @param loaderClassLoader the loader plugin's classloader (setup and created by the platform)
     * @param jarResourcePath the path to the jar-in-jar resource within the loader jar
     * @throws LoadingException if something unexpectedly bad happens
     */
    public JarInJarClassLoader(ClassLoader loaderClassLoader, String... jarResourcePath) throws LoadingException {
        super(Arrays.stream(jarResourcePath)
                .map(path -> extractJar(loaderClassLoader, path))
                .toArray(URL[]::new), loaderClassLoader);
    }

    public void addJarToClasspath(URL url) {
        addURL(url);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass == null) {
                loadedClass = shouldLoadParentFirst(name) ? loadParentThenChild(name) : loadChildThenParent(name);
            }

            if (resolve) {
                resolveClass(loadedClass);
            }

            return loadedClass;
        }
    }

    @Override
    public URL getResource(String name) {
        URL resource = findResource(name);
        if (resource != null) {
            return resource;
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Set<URL> resources = new LinkedHashSet<>();
        Enumeration<URL> childResources = findResources(name);
        while (childResources.hasMoreElements()) {
            resources.add(childResources.nextElement());
        }

        Enumeration<URL> parentResources = getParent() == null
                ? ClassLoader.getSystemResources(name)
                : getParent().getResources(name);
        while (parentResources.hasMoreElements()) {
            resources.add(parentResources.nextElement());
        }

        return Collections.enumeration(resources);
    }

    private Class<?> loadChildThenParent(String name) throws ClassNotFoundException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException ignored) {
            return super.loadClass(name, false);
        }
    }

    private Class<?> loadParentThenChild(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name, false);
        } catch (ClassNotFoundException ignored) {
            return findClass(name);
        }
    }

    private static boolean shouldLoadParentFirst(String className) {
        for (String prefix : PARENT_FIRST_PACKAGES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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

    /**
     * Extracts the "jar-in-jar" from the loader plugin into a temporary file,
     * then returns a URL that can be used by the {@link JarInJarClassLoader}.
     *
     * @param loaderClassLoader the classloader for the "host" loader plugin
     * @param jarResourcePath the inner jar resource path
     * @return a URL to the extracted file
     */
    private static URL extractJar(ClassLoader loaderClassLoader, String jarResourcePath) throws LoadingException {
        // get the jar-in-jar resource
        URL jarInJar = loaderClassLoader.getResource(jarResourcePath);
        if (jarInJar == null) {
            throw new LoadingException("Could not locate jar-in-jar");
        }

        // create a temporary file
        // on posix systems; by default, this is only read/writable by the process owner
        Path path;
        try {
            path = Files.createTempFile(jarResourcePath, ".jar.tmp");
        } catch (IOException e) {
            throw new LoadingException("Unable to create a temporary file", e);
        }

        // mark that the file should be deleted on exit
        path.toFile().deleteOnExit();

        // copy the jar-in-jar to the temporary file path
        try (InputStream in = jarInJar.openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new LoadingException("Unable to copy jar-in-jar to temporary path", e);
        }

        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new LoadingException("Unable to get URL from path", e);
        }
    }

}
