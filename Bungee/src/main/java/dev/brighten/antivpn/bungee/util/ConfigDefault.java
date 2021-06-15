package dev.brighten.antivpn.bungee.util;

import dev.brighten.antivpn.bungee.BungeePlugin;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ConfigDefault<A> {

    private final A defaultValue;
    private final String path;
    private final BungeePlugin plugin;

    public A get() {
        if(plugin.getConfig().get(path) != null)
            return (A) plugin.getConfig().get(path);
        else {
            plugin.getConfig().set(path, defaultValue);
            plugin.getConfig().save();
            return defaultValue;
        }
    }

    public A set(A value) {
        plugin.getConfig().set(path, value);

        return value;
    }
}
