package dev.brighten.antivpn.bukkit;

import dev.brighten.antivpn.utils.load.JarInJarClassLoader;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitLoaderPlugin extends JavaPlugin {

    public BukkitLoaderPlugin() {
        JarInJarClassLoader loader = new JarInJarClassLoader(getClassLoader());

        loader.instantiatePlugin("dev.brighten.antivpn.bukkit.BukkitBootstrap", JavaPlugin.class, this);
    }
}
