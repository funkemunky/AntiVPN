package dev.brighten.antivpn.api;

import lombok.Getter;

public enum ResultType {
    ALLOWED(false),
    WHITELISTED(false),
    DENIED_COUNTRY(true),
    DENIED_PROXY(true),
    UNKNOWN(false);

    @Getter
    private final boolean shouldBlock;

    ResultType(boolean shouldBlock) {
        this.shouldBlock = shouldBlock;
    }
}
