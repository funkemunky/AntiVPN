package dev.brighten.antivpn.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.regex.Pattern;

public class MiscUtils {

    private static final Pattern ipv4 = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");

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

    public static boolean isIpv4(String ip)
    {
        return ipv4.matcher(ip).matches();
    }
}
