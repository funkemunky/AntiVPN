package dev.brighten.antivpn.bungee.util;

import com.google.common.io.ByteStreams;
import dev.brighten.antivpn.bungee.BungeePlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Author: nitramleo (Martin)
 * Date created: 10-Aug-18
 */
public class Config {

    private File file;
    private Configuration configuration;

    public Config() {
        this.file = new File(BungeePlugin.pluginInstance.getDataFolder(), "config.yml");
        try {
            if (!this.file.exists()) {
                if (!BungeePlugin.pluginInstance.getDataFolder().exists()) {
                    BungeePlugin.pluginInstance.getDataFolder().mkdir();
                }
                this.file.createNewFile();
                try (final InputStream is = BungeePlugin.pluginInstance.getResourceAsStream("config.yml");
                     final OutputStream os = new FileOutputStream(this.file)) {
                    ByteStreams.copy(is, os);
                }
            }
            this.configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        this.file = new File(BungeePlugin.pluginInstance.getDataFolder(), "config.yml");
        try {
            this.configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            ConfigurationProvider.getProvider( YamlConfiguration.class).save(this.configuration, this.file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public File getFile() {
        return this.file;
    }

    public double getDouble(final String path) {
        if (this.configuration.get(path) != null) {
            return this.configuration.getDouble(path);
        }
        return 0.0;
    }

    public int getInt(final String path) {
        if (this.configuration.get(path) != null) {
            return this.configuration.getInt(path);
        }
        return 0;
    }

    public Object get(final String path) {
        return this.configuration.get(path);
    }

    public void set(final String path, final Object object) {
        configuration.set(path, object);
    }

    public boolean getBoolean(final String path) {
        return this.configuration.get(path) != null && this.configuration.getBoolean(path);
    }

    public String getString(final String path) {
        if (this.configuration.get(path) != null) {
            return ChatColor.translateAlternateColorCodes('&', this.configuration.getString(path));
        }
        return "String at path: " + path + " not found!";
    }

    public List<String> getStringList(final String path) {
        if (this.configuration.get(path) != null) {
            final ArrayList<String> strings = new ArrayList<String>();
            for (final String string : this.configuration.getStringList(path)) {
                strings.add(ChatColor.translateAlternateColorCodes('&', string));
            }
            return strings;
        }
        return Arrays.asList("String List at path: " + path + " not found!");
    }
}
