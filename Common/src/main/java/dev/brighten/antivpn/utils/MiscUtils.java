package dev.brighten.antivpn.utils;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.utils.json.JSONObject;
import dev.brighten.antivpn.utils.json.JsonReader;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

public class MiscUtils {

    private static final Pattern ipv4 = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");

    public static void close(Closeable... closeables) {
        try {
            for (Closeable closeable : closeables) if (closeable != null) closeable.close();
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    public static void close(AutoCloseable... closeables) {
        try {
            for (AutoCloseable closeable : closeables) if (closeable != null) closeable.close();
        } catch (Exception e) {
            AntiVPN.getInstance().getExecutor().logException(e);
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
            AntiVPN.getInstance().getExecutor().logException(e);
        }
    }

    public static ThreadFactory createThreadFactory(String threadName) {
        return r -> {
            Thread thread = new Thread(r);
            thread.setName(threadName);
            return thread;
        };
    }

    public static UUID formatFromMojangUUID(String mojangUUID) {
        StringBuilder uuid = new StringBuilder();
        for(int i = 0; i <= 31; i++) {
            uuid.append(mojangUUID.charAt(i));
            if(i == 7 || i == 11 || i == 15 || i == 19) {
                uuid.append("-");
            }
        }

        return UUID.fromString(uuid.toString());
    }

    public static UUID lookupUUID(String playername) {
        try {
            JSONObject object = JsonReader
                    .readJsonFromUrl("https://funkemunky.cc/mojang/uuid?name=" + playername);

            if(object.has("id")) {
                return formatFromMojangUUID(object.getString("uuid"));
            }
        } catch (IOException | JSONException e) {
            AntiVPN.getInstance().getExecutor().logException("Error while looking up UUID for " + playername, e);
        }

        return null;
    }

    public static boolean isIpv4(String ip)
    {
        return ipv4.matcher(ip).matches();
    }
}
