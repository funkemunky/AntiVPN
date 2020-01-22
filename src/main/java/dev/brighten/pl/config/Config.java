package dev.brighten.pl.config;

import cc.funkemunky.api.utils.ConfigSetting;
import cc.funkemunky.api.utils.Init;
import cc.funkemunky.api.utils.Priority;

import java.util.ArrayList;
import java.util.List;

@Init(priority = Priority.HIGH)
public class Config {
    @ConfigSetting(name = "license")
    public static String license = "";

    @ConfigSetting(name = "kick-players")
    public static boolean kickPlayers = true;

    @ConfigSetting(name = "kick-message")
    public static String kickMessage = "";

    @ConfigSetting(name = "kick-commands")
    public static List<String> kickCommands = new ArrayList<>();

    @ConfigSetting(name = "kick-bungee")
    public static boolean kickBungee = false;

    @ConfigSetting(name = "alert-staff")
    public static boolean alertStaff = true;

    @ConfigSetting(name = "alert-message")
    public static String alertMessage = "";

    @ConfigSetting(name = "alert-hover")
    public static List<String> alertHoverMessage = new ArrayList<>();

    @ConfigSetting(name = "alert-bungee")
    public static boolean alertBungee = false;

    @ConfigSetting(name = "hide-ips")
    public static boolean hideIps = false;

    @ConfigSetting(name = "fire-event")
    public static boolean fireEvent = true;

    /* Database Stuff */
    @ConfigSetting(path = "database.general", name = "hashIp")
    public static boolean hashIp = true;

    @ConfigSetting(path = "database.general", name = "hashType")
    public static String hashType = "SHA2";
}
