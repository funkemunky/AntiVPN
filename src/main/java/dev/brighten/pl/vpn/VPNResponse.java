package dev.brighten.pl.vpn;

import dev.brighten.db.db.StructureSet;
import dev.brighten.db.utils.json.JSONException;
import dev.brighten.db.utils.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VPNResponse {
    private String ip, countryName, countryCode, method, city, isp, timeZone;
    private boolean proxy, usedAdvanced, cached, success;
    private long cacheTime;
    private int queriesLeft;

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
        json.put("cacheTime", cacheTime);
        json.put("timeZone", timeZone);
        json.put("success", true);
        json.put("queriesLeft", queriesLeft);
        json.put("usedAdvanced", usedAdvanced);
        json.put("cached", cached);

        return json;
    }

    public static VPNResponse fromJson(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);

        return new VPNResponse(jsonObject.getString("ip"), jsonObject.getString("countryName"),
                jsonObject.getString("countryCode"),
                jsonObject.has("method") ? jsonObject.getString("method") : "N/A",
                jsonObject.getString("city"), jsonObject.getString("isp"),
                jsonObject.getString("timeZone"),
                jsonObject.getBoolean("proxy"), jsonObject.getBoolean("usedAdvanced"),
                jsonObject.getBoolean("cached"), jsonObject.getBoolean("success"), -1,
                jsonObject.getInt("queriesLeft"));
    }

    public static VPNResponse fromSet(StructureSet set) {
        return new VPNResponse(set.getObject("ip"), set.getObject("countryName"),
                set.getObject("countryCode"), set.contains("method") ? set.getObject("method") : "N/A",
                set.getObject("city"), set.getObject("isp"),
                set.getObject("timeZone"),
                set.getObject("proxy"), set.getObject("usedAdvanced"),
                set.getObject("cached"), set.getObject("success"),
                set.contains("cacheTime") ? set.getObject("cacheTime") : 01,
                set.getObject("queriesLeft"));
    }
}
