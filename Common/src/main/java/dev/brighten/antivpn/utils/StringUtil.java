package dev.brighten.antivpn.utils;

import dev.brighten.antivpn.AntiVPN;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringUtil {
    public static String line(String color) {
        return color + "&m-----------------------------------------------------";
    }

    public static String line() {
        return "&m-----------------------------------------------------";
    }

    public static String getHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-128");
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
}
