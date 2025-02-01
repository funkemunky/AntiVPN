package dev.brighten.antivpn.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class IpUtils {
    public static Optional<BigDecimal> getIpDecimal(String address) {
        try {
            InetAddress inet = InetAddress.getByName(address);

            if(inet instanceof Inet4Address) {
                return Optional.of(BigDecimal.valueOf(ipv4ToLong(address)));
            } return Optional.of(new BigDecimal(ipv6ToDecimalFormat(address)));
        } catch(Exception e) {
            return Optional.empty();
        }
    }

    public static long ipv4ToLong(String address) {
        String[] addrArray = address.split("\\.");

        long ipDecimal = 0;

        for (int i = 0; i < addrArray.length; i++) {

            int power = 3 - i;
            ipDecimal += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
        }

        return ipDecimal;
    }

    public static String getIpv4(long ip) {
        StringBuilder sb = new StringBuilder(15);

        for (int i = 0; i < 4; i++) {
            sb.insert(0, ip & 0xff);

            if (i < 3) {
                sb.insert(0, '.');
            }

            ip >>= 8;
        }

        return sb.toString();
    }

    public static boolean isIpv4(BigDecimal ip) {
        return ip.compareTo(BigDecimal.valueOf(4294967295L)) <= 0;
    }

    public static boolean isIpv6(BigDecimal ip) {
        return ip.compareTo(BigDecimal.valueOf(4294967295L)) > 0;
    }
    public static boolean isIpv4(String ip) {
        return ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    }

    public static boolean isNotIp(String ip) {
        return !isIpv4(ip) && !isIpv6(ip);
    }

    public static boolean isIpv6(String ip) {
        return ip.matches("^([0-9a-fA-F]{1,4}:){7}([0-9a-fA-F]{1,4}|:)$|^(([0-9a-fA-F]{1,4}:){0,6}([0-9a-fA-F]{1,4}|:))?(::([0-9a-fA-F]{1,4}:){0,5}([0-9a-fA-F]{1,4}|:))?$");
    }

    public static String getIpv4(BigDecimal ip) {
        try {
            return Inet4Address.getByAddress(ip.toBigInteger().toByteArray()).getHostAddress();
        } catch (UnknownHostException e) {
            return "Error";
        }
    }

    public static String getIpv6(BigDecimal ip) {
        try {
            return Inet6Address.getByAddress(ip.toBigInteger().toByteArray()).getHostAddress();
        } catch (UnknownHostException e) {
            return "Error";
        }
    }

    public static BigInteger ipv6ToDecimalFormat(String ipAddress) throws UnknownHostException {
        return new BigInteger(1, Inet6Address.getByName(ipAddress).getAddress());
    }

}
