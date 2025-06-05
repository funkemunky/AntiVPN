package dev.brighten.antivpn.database.mongodb.records;

import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.web.objects.VPNResponse;

import java.util.Date;

public record UserIpResponse(String ip, Date queried, String response) {

    public VPNResponse getVpnResponse() throws JSONException {
        return VPNResponse.fromJson(response);
    }
}
