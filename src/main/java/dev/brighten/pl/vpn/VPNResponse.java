package dev.brighten.pl.vpn;

import cc.funkemunky.carbon.db.StructureSet;
import cc.funkemunky.carbon.utils.json.JSONException;
import cc.funkemunky.carbon.utils.json.JSONObject;
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
                jsonObject.getBoolean("cached"), jsonObject.getBoolean("sucess"),
                jsonObject.has("score") ? jsonObject.getDouble("score") : -1,
                jsonObject.getInt("queriesLeft"));
    }

    public static VPNResponse fromSet(StructureSet set) {
        return new VPNResponse(set.getField("ip"), set.getField("countryName"),
                set.containsKey("method") ? set.getField("method") : "N/A",
                set.getField("countryCode"), set.getField("state"),
                set.getField("city"), set.getField("isp"),
                set.getField("timeZone"), set.getField("locationString"),
                set.getField("proxy"), set.getField("usedAdvanced"),
                set.getField("cached"), set.getField("sucess"),
                set.containsKey("score") ? set.getDouble("score") : -1,
                set.getField("queriesLeft"));
    }
}
