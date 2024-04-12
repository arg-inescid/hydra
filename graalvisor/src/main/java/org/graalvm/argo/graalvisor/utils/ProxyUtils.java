package org.graalvm.argo.graalvisor.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpExchange;

public class ProxyUtils {

    /**
     * Each thread has it onw buffer top avoid the GC;
     */
    private static final ThreadLocal<byte[]> contextBuffer = ThreadLocal.withInitial(() -> new byte[1024]);



    public static void writeResponse(HttpExchange t, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(code, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    public static void errorResponse(HttpExchange t, String errorMessage) throws IOException {
        writeResponse(t, 502, "ERROR: " + errorMessage);
    }

    public static String extractRequestBody(HttpExchange t) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try {
            for (int length; (length = t.getRequestBody().read(contextBuffer.get())) != -1;) {
                result.write(contextBuffer.get(), 0, length);
            }
            return result.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}
