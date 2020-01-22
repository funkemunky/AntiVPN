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
    private String ip, countryName, countryCode, method, state, city, isp, timeZone, locationString;
    private boolean proxy, usedAdvanced, cached, success;
    private double score;
    private int queriesLeft;

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("ip", ip);
        json.put("countryName", countryName);
        json.put("countryCode", countryCode);
        json.put("state", state);
        json.put("city", city);
        json.put("method", method);
        json.put("isp", isp);
        json.put("score", score);
        json.put("proxy", proxy);
        json.put("success", success);
        json.put("timeZone", timeZone);
        json.put("success", true);
        json.put("queriesLeft", queriesLeft);
        json.put("locationString", locationString);
        json.put("usedAdvanced", usedAdvanced);
        json.put("cached", cached);

        return json;
    }

    public static VPNResponse fromJson(String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);

        return new VPNResponse(jsonObject.getString("ip"), jsonObject.getString("countryName"),
                jsonObject.has("method") ? jsonObject.getString("method") : "N/A",
                jsonObject.getString("countryCode"), jsonObject.getString("state"),
                jsonObject.getString("city"), jsonObject.getString("isp"),
                jsonObject.getString("timeZone"), jsonObject.getString("locationString"),
                jsonObject.getBoolean("proxy"), jsonObject.getBoolean("usedAdvanced"),
                jsonObject.getBoolean("cached"), jsonObject.getBoolean("success"),
                jsonObject.has("score") ? jsonObject.getDouble("score") : -1,
                jsonObject.getInt("queriesLeft"));
    }

    public static VPNResponse fromSet(StructureSet set) {
        return new VPNResponse(set.getObject("ip"), set.getObject("countryName"),
                set.contains("method") ? set.getObject("method") : "N/A",
                set.getObject("countryCode"), set.getObject("state"),
                set.getObject("city"), set.getObject("isp"),
                set.getObject("timeZone"), set.getObject("locationString"),
                set.getObject("proxy"), set.getObject("usedAdvanced"),
                set.getObject("cached"), set.getObject("success"),
                set.contains("score") ? (double)set.getObject("score") : -1,
                set.getObject("queriesLeft"));
    }
}
