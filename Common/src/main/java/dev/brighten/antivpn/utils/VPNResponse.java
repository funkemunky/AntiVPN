package dev.brighten.antivpn.utils;

import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.utils.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VPNResponse {
    private String asn, ip, countryName, countryCode, city, timeZone, method, isp;
    private boolean proxy, cached, success;
    private double latitude, longitude;
    private long lastAccess;
    private long queriesLeft;

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("ip", ip);
        json.put("countryName", countryName);
        json.put("countryCode", countryCode);
        json.put("city", city);
        json.put("method", method);
        json.put("isp", isp);
        json.put("proxy", proxy);
        json.put("success", success);
        json.put("timeZone", timeZone);
        json.put("success", true);
        json.put("queriesLeft", queriesLeft);
        json.put("cached", cached);

        return json;
    }

    public static VPNResponse fromJson(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);

        return new VPNResponse(jsonObject.getString("asn"), jsonObject.getString("ip"),
                jsonObject.getString("countryName"), jsonObject.getString("countryCode"),
                jsonObject.getString("city"), jsonObject.getString("timeZone"),
                jsonObject.has("method") ? jsonObject.getString("method") : "N/A",
                jsonObject.getString("isp"), jsonObject.getBoolean("proxy"),
                jsonObject.getBoolean("cached"), jsonObject.getBoolean("success"),
                jsonObject.getDouble("latitude"), jsonObject.getDouble("longitude"),
                jsonObject.getLong("lastAccess"), jsonObject.getInt("queriesLeft"));
    }
}
