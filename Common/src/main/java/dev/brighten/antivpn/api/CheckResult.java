package dev.brighten.antivpn.api;

import dev.brighten.antivpn.web.objects.VPNResponse;

public record CheckResult(VPNResponse response, ResultType resultType) {
}
