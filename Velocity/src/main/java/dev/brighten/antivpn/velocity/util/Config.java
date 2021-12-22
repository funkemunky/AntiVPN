package dev.brighten.antivpn.velocity.util;

import com.google.common.io.ByteStreams;
import dev.brighten.antivpn.velocity.VelocityPlugin;
import dev.brighten.antivpn.velocity.config.Configuration;
import dev.brighten.antivpn.velocity.config.ConfigurationProvider;
import dev.brighten.antivpn.velocity.config.YamlConfiguration;

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
        File dataFolder = VelocityPlugin.INSTANCE.getConfigDir().toFile();
        this.file = new File(dataFolder, "config.yml");
        try {
            if (!this.file.exists()) {
                if (!dataFolder.exists()) {
                    dataFolder.mkdir();
                }
                this.file.createNewFile();
            }
            this.configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(this.file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void load() {
        File dataFolder = VelocityPlugin.INSTANCE.getConfigDir().toFile();
        this.file = new File(dataFolder, "config.yml");
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
            return StringUtils.translateAlternateColorCodes('&', this.configuration.getString(path));
        }
        return "String at path: " + path + " not found!";
    }

    public List<String> getStringList(final String path) {
        if (this.configuration.get(path) != null) {
            final ArrayList<String> strings = new ArrayList<String>();
            for (final String string : this.configuration.getStringList(path)) {
                strings.add(StringUtils.translateAlternateColorCodes('&', string));
            }
            return strings;
        }
        return Arrays.asList("String List at path: " + path + " not found!");
    }
}
