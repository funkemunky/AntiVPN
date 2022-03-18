package dev.brighten.antivpn.bukkit.util;

import dev.brighten.antivpn.utils.MiscUtils;
import dev.brighten.antivpn.utils.config.Configuration;
import lombok.AllArgsConstructor;
import org.bukkit.plugin.Plugin;

import java.io.File;

@AllArgsConstructor
public class ConfigDefault<A> {

    private final A defaultValue;
    private final String path;
    private final Plugin plugin;

    public A get() {
        if(plugin.getConfig().get(path) != null)
            return (A) plugin.getConfig().get(path);
        else {
            plugin.getConfig().set(path, defaultValue);
            plugin.saveConfig();
            return defaultValue;
        }
    }

    public A set(A value) {
        plugin.getConfig().set(path, value);

        return value;
    }
}
