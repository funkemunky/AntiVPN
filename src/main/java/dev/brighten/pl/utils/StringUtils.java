package dev.brighten.pl.utils;

import cc.funkemunky.api.utils.Color;
import dev.brighten.db.utils.json.JSONException;
import dev.brighten.pl.vpn.VPNResponse;
import lombok.val;

public class StringUtils {

    public static String formatString(String string, VPNResponse response) {
        String message = Color.translate(string);
        if (response.isSuccess()) {

            try {
                val json = response.toJson();

                for (String key : json.keySet()) {
                    message = message.replace("%" + key + "%", String.valueOf(json.get(key)));
                }
                return message;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return message;
    }
}
