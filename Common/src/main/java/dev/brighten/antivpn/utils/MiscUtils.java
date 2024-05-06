package dev.brighten.antivpn.utils;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

public class MiscUtils {

    private static final Pattern ipv4 = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

    public static void close(Closeable... closeables) {
        try {
            for (Closeable closeable : closeables) if (closeable != null) closeable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void close(AutoCloseable... closeables) {
        try {
            for (AutoCloseable closeable : closeables) if (closeable != null) closeable.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            int lenght;
            byte[] buf = new byte[1024];

            while ((lenght = in.read(buf)) > 0)
            {
                out.write(buf, 0, lenght);
            }

            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ThreadFactory createThreadFactory(String threadName) {
        return r -> {
            Thread thread = new Thread(r);
            thread.setName(threadName);
            return thread;
        };
    }

    public static boolean isIpv4(String ip)
    {
        return ipv4.matcher(ip).matches();
    }

    public static boolean isIpv6(String ip)
    {
        try {
            InetAddress address = InetAddress.getByName(ip);

            return address instanceof Inet6Address;
        } catch(Exception e) {
            return false;
        }
    }

    //Ipv4 to decimal
    public static long ipv4ToLong(String address) {
        String[] addrArray = address.split("\\.");

        long ipDecimal = 0;

        for (int i = 0; i < addrArray.length; i++) {

            int power = 3 - i;
            ipDecimal += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
        }

        return ipDecimal;
    }

    //Ipv6 to decimal
    public static BigDecimal ipv6ToDecimalFormat(String address) {
        try {
            InetAddress inet = InetAddress.getByName(address);

            if(inet instanceof Inet6Address) {
                byte[] bytes = inet.getAddress();
                BigInteger bigInt = new BigInteger(1, bytes);
                return new BigDecimal(bigInt);
            } return BigDecimal.valueOf(-1);
        } catch(Exception e) {
            return BigDecimal.valueOf(-1);
        }
    }

    //decimal to ipv6
    public static String decimalToIpv6(BigDecimal ip) {
        byte[] bytes = ip.toBigInteger().toByteArray();
        try {
            InetAddress address = InetAddress.getByAddress(bytes);
            return address.getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    //decimal to ipv4
    public static String decimalToIpv4(long ip) {
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
}
