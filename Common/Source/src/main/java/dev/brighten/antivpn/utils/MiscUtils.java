/*
 * Copyright 2026 Dawson Hessler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.brighten.antivpn.utils;

import dev.brighten.antivpn.AntiVPN;
import dev.brighten.antivpn.utils.json.JSONException;
import dev.brighten.antivpn.utils.json.JSONObject;
import dev.brighten.antivpn.utils.json.JsonReader;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

public class MiscUtils {

    private static final Pattern ipv4 = Pattern.compile("[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}");
    private static final String DEFAULT_FUNKEMUNKY_UUID_ENDPOINT = "https://funkemunky.cc/mojang/uuid?name=";
    private static final String DEFAULT_MOJANG_UUID_ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/";
    private static volatile String funkemunkyUuidEndpoint = DEFAULT_FUNKEMUNKY_UUID_ENDPOINT;
    private static volatile String mojangUuidEndpoint = DEFAULT_MOJANG_UUID_ENDPOINT;

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

    public static List<CIDRUtils> rangeToCidrs(BigInteger start, BigInteger end) throws UnknownHostException {
        List<CIDRUtils> cidrs = new ArrayList<>();

        while (start.compareTo(end) <= 0) {
            // Find the number of trailing zero bits — this determines max block size alignment
            int trailingZeros = start.equals(BigInteger.ZERO)
                    ? 128  // handle the edge case
                    : start.getLowestSetBit();

            // Find the largest block that fits
            BigInteger remaining = end.subtract(start).add(BigInteger.ONE);
            int maxBits = remaining.bitLength() - 1;

            int blockBits = Math.min(trailingZeros, maxBits);
            int prefixLen = 32 - blockBits; // use 128 for IPv6

            // Build the CIDR string
            byte[] addrBytes = toFixedLengthBytes(start); // use 16 for IPv6
            String cidr = InetAddress.getByAddress(addrBytes).getHostAddress() + "/" + prefixLen;
            cidrs.add(new CIDRUtils(cidr));

            // Advance past this block
            start = start.add(BigInteger.ONE.shiftLeft(blockBits));
        }

        return cidrs;
    }

    private static byte[] toFixedLengthBytes(BigInteger value) {
        byte[] raw = value.toByteArray();
        byte[] result = new byte[4];
        int srcPos = Math.max(0, raw.length - 4);
        int destPos = Math.max(0, 4 - raw.length);
        System.arraycopy(raw, srcPos, result, destPos, Math.min(raw.length, 4));
        return result;
    }

    public static UUID lookupUUID(String playername) {
        try {
            UUID uuid = lookupUuidFromUrl(funkemunkyUuidEndpoint + playername);
            if (uuid != null) {
                return uuid;
            }
        } catch (IOException | JSONException | URISyntaxException e) {
            AntiVPN.getInstance().getExecutor().logException("Error while looking up UUID for " + playername + "! Falling back to Mojang API", e);
            return lookupMojangUuid(playername);
        }

        return null;
    }

    private static UUID lookupUuidFromUrl(String url) throws IOException, JSONException, URISyntaxException {
        HttpURLConnection connection = (HttpURLConnection) new URI(url).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setInstanceFollowRedirects(true);

        int responseCode = connection.getResponseCode();
        if (responseCode >= 500) {
            throw new IOException("Server returned HTTP " + responseCode + " for " + url);
        }
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return null;
        }

        try (InputStream inputStream = connection.getInputStream()) {
            JSONObject object = new JSONObject(JsonReader.readAll(new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8)));
            if (object.has("uuid")) {
                return parseUuid(object.getString("uuid"));
            }
            if (object.has("id")) {
                return parseUuid(object.getString("id"));
            }
        }

        return null;
    }

    private static UUID parseUuid(String value) {
        if (value.length() == 32) {
            value = value.replaceFirst(
                    "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                    "$1-$2-$3-$4-$5"
            );
        }

        return UUID.fromString(value);
    }

    private static UUID lookupMojangUuid(String playerName) {
        try {
            return lookupUuidFromUrl(mojangUuidEndpoint + playerName);
        } catch (IOException | JSONException | URISyntaxException e) {
            AntiVPN.getInstance().getExecutor().logException("Error while looking up UUID for " + playerName + " from Mojang!:", e);
        }

        return null;
    }

    static void setLookupEndpointsForTesting(String funkemunkyEndpoint, String mojangEndpoint) {
        funkemunkyUuidEndpoint = funkemunkyEndpoint;
        mojangUuidEndpoint = mojangEndpoint;
    }

    static void resetLookupEndpointsForTesting() {
        funkemunkyUuidEndpoint = DEFAULT_FUNKEMUNKY_UUID_ENDPOINT;
        mojangUuidEndpoint = DEFAULT_MOJANG_UUID_ENDPOINT;
    }
    public static boolean isIpv4(String ip)
    {
        return ipv4.matcher(ip).matches();
    }
}
