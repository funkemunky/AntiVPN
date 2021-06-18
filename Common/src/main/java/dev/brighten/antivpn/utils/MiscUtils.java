package dev.brighten.antivpn.utils;

import java.io.Closeable;

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

}
