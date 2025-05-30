package dev.brighten.antivpn.utils;

import dev.brighten.antivpn.AntiVPN;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dev.brighten.antivpn.api.APIPlayer;
import dev.brighten.antivpn.web.objects.VPNResponse;

public class StringUtil {
    public static String line(String color) {
        return color + "&m-----------------------------------------------------";
    }

    public static String line() {
        return "&m-----------------------------------------------------";
    }

    public static String getHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
        return null;
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
