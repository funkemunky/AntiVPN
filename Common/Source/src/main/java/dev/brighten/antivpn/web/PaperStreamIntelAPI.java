/*
 * Copyright 2026 LucasTHCR
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.brighten.antivpn.web;

import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.utils.json.JSONObject;
import dev.brighten.antivpn.utils.json.JsonReader;
import dev.brighten.antivpn.web.objects.VPNResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PaperStreamIntelAPI {

  public static VPNResponse getVPNResponse(String ip) throws JSONException, IOException {
    JSONObject result =
        JsonReader.readJsonFromUrl(
            "https://intel.paperstream.dev/v1/ip/" + URLEncoder.encode(ip, StandardCharsets.UTF_8.name()));
    return fromJson(ip, result);
  }

  public static VPNResponse fromJson(String ip, JSONObject result) throws JSONException {
    if (result.has("error")) {
      return VPNResponse.builder()
          .success(false)
          .failureReason("PaperStream Intel: " + result.getString("error"))
          .build();
    }

    boolean proxy =
        result.optBoolean("vpn") || result.optBoolean("proxy") || result.optBoolean("tor");
    String method =
        result.optBoolean("tor") ? "tor" : result.optBoolean("vpn") ? "vpn" : proxy ? "proxy" : "N/A";
    String country = result.isNull("country") ? "N/A" : result.optString("country", "N/A");

    return VPNResponse.builder()
        .ip(ip)
        .asn(result.isNull("asn") ? "N/A" : String.valueOf(result.opt("asn")))
        .countryName(country)
        .countryCode(country)
        .city("N/A")
        .timeZone("N/A")
        .method("PaperStream Intel (" + method + ")")
        .isp("N/A")
        .failureReason("N/A")
        .proxy(proxy)
        .cached(false)
        .success(true)
        .lastAccess(System.currentTimeMillis())
        .queriesLeft(-1)
        .build();
  }
}
