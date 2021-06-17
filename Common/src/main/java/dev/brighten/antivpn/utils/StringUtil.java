package dev.brighten.antivpn.utils;

public class StringUtil {
    public static String line(String color) {
        return color + "&m-----------------------------------------------------";
    }

    public static String line() {
        return "&m-----------------------------------------------------";
    }

    public static String lineNoStrike(String color) {
        return color + "-----------------------------------------------------";
    }
}
