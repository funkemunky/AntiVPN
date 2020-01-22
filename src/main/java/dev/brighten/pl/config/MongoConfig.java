package dev.brighten.pl.config;

import cc.funkemunky.api.utils.ConfigSetting;
import cc.funkemunky.api.utils.Init;
import cc.funkemunky.api.utils.Priority;

@Init(priority = Priority.LOW)
public class MongoConfig {

    @ConfigSetting(path = "database.mongo", name = "enabled")
    public static boolean enabled = false;

    @ConfigSetting(path = "database.mongo", name = "ip")
    public static String ip = "127.0.0.1";

    @ConfigSetting(path = "database.mongo", name = "port")
    public static int port = 27017;

    @ConfigSetting(path = "database.mongo", name = "username")
    public static String username = "";

    @ConfigSetting(path = "database.mongo", name = "password")
    public static String password = "";

    @ConfigSetting(path = "database.mongo", name = "database")
    public static String database = "kaurivpn";

    @ConfigSetting(path = "database.mongo", name = "authDatabase")
    public static String authDatabase = "";
}
