package dev.brighten.antivpn.utils;

import java.io.*;
import java.util.concurrent.ThreadFactory;
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
}
