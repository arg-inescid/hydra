package org.graalvm.argo.lambda_manager.utils;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LocalConnection implements LambdaConnection {

    // Lambda we are connecting from.
    private final Lambda lambda;

    // Open channel to read and write.
    private final ConcurrentLinkedQueue<SocketChannel> channels;

    // Buffer used read data into.
    private final ThreadLocal<ByteBuffer> tlBuffer = new ThreadLocal<>();

    public LocalConnection(Lambda lambda) {
        this.lambda = lambda;
        this.channels = new ConcurrentLinkedQueue<>();
    }

    private String udsPath() {
        return Environment.CODEBASE + "/" + lambda.getLambdaName() + "/lambda.uds";
    }

    private boolean waitForUDS() {
        // Wait at least 20 times (100 ms), i.e., 2 seconds.
        for (int attempts = 0; attempts < 20; attempts++) {
            if (Files.notExists(Path.of(udsPath()))) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }

        return Files.exists(Path.of(udsPath()));
    }

    // TODO - this could be the uds existing. Then, we could have another (responsive/ping  that actually does this.)
    public boolean waitUntilReady() {
        String payload = "{ \"act\":\"list_ep\" }";

        // If the file does not exist, quit.
        if (!waitForUDS()) {
            return false;
        }

        // Sending a simple list command to check if the server is up.
        sendRequest("/command", payload.getBytes(), null, 0);
        return true;
     }

    private SocketChannel getChannel() {
        SocketChannel channel = channels.poll();

        if (channel == null) {
            try {
                channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                channel.connect(UnixDomainSocketAddress.of(udsPath()));
            } catch (Exception e) {
                Logger.log(Level.WARNING, e.getMessage(), e);
                return null;
            }
        }
        return channel;
    }

    private void returnChannel(SocketChannel channel) {
        if (channel != null) {
            channels.offer(channel);
        }
    }

    private HashMap<?, ?> parseJSON(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, HashMap.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void waitForIsolate(SocketChannel channel, String responseJSON, long timeout) {
        int isolate = (int) parseJSON(responseJSON).get("isolate");
        String path = String.format("/exitcode?%d", isolate);
        byte[] payload = new byte[0];
        long sleepBetweenAttempts = 10;
        long maxAttempts = timeout * 1000 / sleepBetweenAttempts;

        for (int attempts = 0; attempts < maxAttempts; attempts++) {
            String response = sendRequest(channel, "GET", path, payload, 0);
            int code = Integer.parseInt(response.split("\n")[0].split(" ")[1]);
            if (code != 404) {
                return;
            } else {
                try {
                    Thread.sleep(sleepBetweenAttempts);
                } catch (InterruptedException e) {
                    // Ignored.
                }
            }
        }
        Logger.log(Level.WARNING, String.format("Waiting for isolate %d timedout!", isolate));
    }

    private String sendRequest(SocketChannel channel, String requestType, String path, byte[] payload, long timeout) {
        String response;
        int responseCode;
        String responseJSON;
        ByteBuffer buffer = tlBuffer.get();
        StringBuilder request = new StringBuilder();
        request.append(requestType).append(" ").append(path).append(" HTTP/1.1\r\n");
        request.append("Host: localhost\r\n");
        request.append("User-Agent: LambdaManager\r\n");
        request.append("Accept: */*\r\n");
        request.append("Content-Length: ").append(payload.length).append("\r\n");
        request.append("Content-Type: application/json\r\n");
        request.append("\r\n");

        // Initialize thread local buffer if needed.
        if (buffer == null) {
            tlBuffer.set(ByteBuffer.allocate(4 * 1024));
            buffer = tlBuffer.get();
        }

        try {
            // Write Header.
            channel.write(ByteBuffer.wrap(request.toString().getBytes(StandardCharsets.UTF_8)));

            // Write payload.
            channel.write(ByteBuffer.wrap(payload));

            // Read Response.
            response = new String(buffer.array(), 0, channel.read(buffer));

            // 1st line example: 'HTTP/1.1 200 OK'
            // 2nd line example: Content-Length: 2
            // 3rd line is empty.
            // 4th line example: '{"ep":"9001","isolate":1,"msg":"isolate active after ready"}'
            String[] responseLines = response.split("\n");
            responseCode = Integer.parseInt(responseLines[0].split(" ")[1]);
            responseJSON = responseLines[responseLines.length - 1];

            // Check the http return code.
            if (!path.contains("/exitcode") && (responseCode < 200 || responseCode > 299)) {
                Logger.log(Level.WARNING,
                    String.format("Received non 2xx code (%d): in lambda %s. Request: path=%s payload=%s; Response: %s",
                        responseCode, lambda.getLambdaID(), path, new String(payload), responseJSON));
            }
            // Discard data.
            buffer.clear();
        } catch (IOException e) {
            Logger.log(Level.WARNING, "Received IOException in lambda " + lambda.getLambdaID() + ". Message: " + e.getMessage());
            return e.getLocalizedMessage();
        }

        // If the request is to create an isolate, wait for it to finish.
        if (payload.length > 0 && parseJSON(new String(payload)).get("act").equals("add_isolate")) {
            waitForIsolate(channel, responseJSON, timeout);
        }

        return response;
    }

    public String sendRequest(String path, byte[] payload, Lambda lambda, long timeout) {
        SocketChannel channel = getChannel();
        String response = sendRequest(channel, "POST", path, payload, timeout);
        returnChannel(channel);

        // Returning the response JSON.
        return response.split("\n")[3];
    }

    public void close() {
        for (SocketChannel channel : channels) {
            try {
                channel.close();
            } catch (IOException e) {
               e.printStackTrace();
            }
        }
    }
}