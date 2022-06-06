package dev.brighten.antivpn.web;

import dev.brighten.antivpn.web.objects.QueryResponse;
import dev.brighten.antivpn.web.objects.VPNResponse;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.utils.json.JSONObject;
import dev.brighten.antivpn.utils.json.JsonReader;

import java.io.IOException;

public class FunkemunkyAPI {

    public static VPNResponse getVPNResponse(String ip, String license, boolean cachedResults /* faster if set to true*/)
            throws JSONException, IOException {
        JSONObject result = JsonReader.readJsonFromUrl(String
                .format("https://funkemunky.cc/vpn?ip=%s&license=%s&cache=%s",
                        ip, license.length() == 0 ? "none" : license, cachedResults));

        return VPNResponse.fromJson(result);
    }

    public static QueryResponse getQueryResponse(String license) throws JSONException, IOException {
        JSONObject result = JsonReader.readJsonFromUrl("https://funkemunky.cc/vpn/queryCheck?license=" + license);

        return QueryResponse.fromJson(result);
    }
}
