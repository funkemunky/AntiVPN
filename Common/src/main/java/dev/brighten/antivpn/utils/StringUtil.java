package dev.brighten.antivpn.utils;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.web.objects.VPNResponse;

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

    public static String varReplace(String input, APIPlayer player, VPNResponse result) {
        return input.replace("%player%", player.getName())
                .replace("%reason%", result.getMethod())
                .replace("%country%", result.getCountryName())
                .replace("%city%", result.getCity());
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();

        for(int i = 0; i < b.length - 1; ++i) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = 167;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }
}
