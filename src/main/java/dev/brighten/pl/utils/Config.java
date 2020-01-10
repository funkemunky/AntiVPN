package dev.brighten.pl.utils;

import cc.funkemunky.api.utils.ConfigSetting;
import cc.funkemunky.api.utils.Init;

import java.util.ArrayList;
import java.util.List;

@Init
public class Config {
    @ConfigSetting(name = "license")
    public static String license = "";

    @ConfigSetting(name = "kick-players")
    public static boolean kickPlayers = true;

    @ConfigSetting(name = "kick-message")
    public static String kickMessage = "";

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
}
