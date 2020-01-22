package dev.brighten.pl.config;

import cc.funkemunky.api.utils.ConfigSetting;
import cc.funkemunky.api.utils.Init;
import cc.funkemunky.api.utils.Priority;

@Init(priority = Priority.LOW)
public class FlatfileConfig {

    @ConfigSetting(path = "database.flatfile", name = "enabled")
    public static boolean enabled = true;

    @ConfigSetting(path = "database.flatfile", name = "database")
    public static String database = "kaurivpn";
}
