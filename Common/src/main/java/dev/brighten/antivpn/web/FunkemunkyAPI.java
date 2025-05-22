package dev.brighten.antivpn.web;

import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.utils.json.JSONObject;
import dev.brighten.antivpn.utils.json.JsonReader;
import dev.brighten.antivpn.web.objects.QueryResponse;
import dev.brighten.antivpn.web.objects.VPNResponse;

import java.io.IOException;

public class FunkemunkyAPI {

    /**
     *
     * Queries <a href="https://funkemunky.cc/vpn">...</a> API and returns information on the IP
     *
     * @param ip String
     * @param license String
     * @param cachedResults boolean
     * @return VPNResponse
     * @throws JSONException Throws when JSON response is not formatted properly.
     * @throws IOException Throws when there is an error connecting to and processing information from API.
     */
    public static VPNResponse getVPNResponse(String ip, String license, boolean cachedResults /* faster if set to true*/)
            throws JSONException, IOException {
        JSONObject result = JsonReader.readJsonFromUrl(String
                .format("https://funkemunky.cc/vpn?ip=%s&license=%s&cache=%s",
                        ip, license.isEmpty() ? "none" : license, cachedResults));

        return VPNResponse.fromJson(result);
    }

    /**
     * Feeds into {@link FunkemunkyAPI#getQueryResponse(String)} using "none" as argument
     * to grab query information based on the connecting IP address.
     *
     * @return QueryResponse
     * @throws JSONException Throws when JSON response is not formatted properly.
     * @throws IOException Throws when there is an error connecting to and processing information from API.
     */
    public static QueryResponse getQueryResponse() throws JSONException, IOException {
        return getQueryResponse("none");
    }

    /**
     * Queries <a href="https://funkemunky.cc/vpn/queryCheck">...</a> and returns information based on the
     * provided licence input.
     *
     * @param license String
     * @return QueryResponse
     * @throws JSONException Throws when JSON response is not formatted properly.
     * @throws IOException Throws when there is an error connecting to and processing information from API.
     */
    public static QueryResponse getQueryResponse(String license) throws JSONException, IOException {
        JSONObject result = JsonReader.readJsonFromUrl("https://funkemunky.cc/vpn/queryCheck?license=" + license);

        return QueryResponse.fromJson(result);
    }
}
