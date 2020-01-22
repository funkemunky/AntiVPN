package dev.brighten.pl.config;

import cc.funkemunky.api.utils.ConfigSetting;
import cc.funkemunky.api.utils.Init;
import cc.funkemunky.api.utils.Priority;

@Init(priority = Priority.LOW)
public class MySQLConfig {

    @ConfigSetting(path = "database.mysql", name = "enabled")
    public static boolean enabled = false;

    @ConfigSetting(path = "database.mysql", name = "ip")
    public static String ip = "127.0.0.1";

    @ConfigSetting(path = "database.mysql", name = "port")
    public static int port = 3306;

    @ConfigSetting(path = "database.mysql", name = "username")
    public static String username = "root";

    @ConfigSetting(path = "database.mysql", name = "password")
    public static String password = "password";

    @ConfigSetting(path = "database.mysql", name = "database")
    public static String database = "kaurivpn";

    @ConfigSetting(path = "database.mysql", name = "ssl")
    public static boolean ssl = true;

}
