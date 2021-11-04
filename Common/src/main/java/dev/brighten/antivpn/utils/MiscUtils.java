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

public class MiscUtils {

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

        try {
            InetAddress address = InetAddress.getByName(ip);

            return address instanceof Inet4Address;
        } catch(Exception e) {
            return false;
        }
    }

    /* Borrowed from FireFlyx ngxdev */
    public static void download(File file, String from) throws Exception {
        URL url = new URL(from);
        InputStream stream = url.openStream();
        ReadableByteChannel channel = Channels.newChannel(stream);
        FileOutputStream out = new FileOutputStream(file);
        out.getChannel().transferFrom(channel, 0L, Long.MAX_VALUE);
    }

    /* Borrowed from FireFlyx ngxdev */
    public static ClassLoader injectorClassLoader = MiscUtils.class.getClassLoader();

    /* Borrowed from FireFlyx ngxdev */
    public static void injectURL(URL url) {
        try {
            URLClassLoader systemClassLoader = (URLClassLoader) injectorClassLoader;
            Class<URLClassLoader> classLoaderClass = URLClassLoader.class;

            try {
                Method method = classLoaderClass.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(systemClassLoader, url);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } catch (Exception e) {
        }
    }
}
