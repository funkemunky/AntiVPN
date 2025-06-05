package dev.brighten.antivpn.database.mongodb.records;

import java.math.BigDecimal;

public record CidrWhitelist(BigDecimal start, BigDecimal end) {
}
