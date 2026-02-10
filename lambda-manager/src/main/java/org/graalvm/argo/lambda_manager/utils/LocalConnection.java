package org.graalvm.argo.lambda_manager.utils;

import java.io.File;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.newsclub.net.unix.AFSocketFactory;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocalConnection implements LambdaConnection {

    // Lambda we are connecting from.
    private final Lambda lambda;

    // Http client that uses unix domain sockets underneath.
    private OkHttpClient client;

    public LocalConnection(Lambda lambda) {
        this.lambda = lambda;
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
        // If the file does not exist, quit.
        if (!waitForUDS()) {
            return false;
        }

        try {
            SocketAddress addr = AFUNIXSocketAddress.of(new File(udsPath()));
            client = new OkHttpClient.Builder()
                .socketFactory(new AFSocketFactory.FixedAddressSocketFactory(addr))
                .callTimeout(Duration.ofMinutes(1))
                .build();
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }

        // Sending a simple list command to check if the server is up.
        sendRequest("command", "{ \"act\":\"list_ep\" }".getBytes(), null, 1);
        return true;
     }

    private HashMap<?, ?> parseJSON(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, HashMap.class);
        } catch (Exception e) {
            Logger.log(Level.WARNING, String.format("Could not parse json in lambda %s: %s", lambda.getLambdaID(), json));
            return null;
        }
    }

    private void waitForIsolate(int isolate, long timeout) {
        long sleepBetweenAttempts = 10;
        long maxAttempts = timeout * 1000 / sleepBetweenAttempts;
        Request request = new Request.Builder().url(String.format("http://localhost/exitcode?%d", isolate)).build();

        for (int attempts = 0; attempts < maxAttempts; attempts++) {
            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 404) {
                    Thread.sleep(sleepBetweenAttempts);
                } else {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Logger.log(Level.WARNING, String.format("Waiting for isolate %d timedout (after %d attempts)!", isolate, maxAttempts));
    }

    private String sendRequest(String path, byte[] payload, long timeout) {
        Request request = new Request.Builder()
            .url(String.format("http://localhost/%s", path))
            .post(RequestBody.create(new String(payload), MediaType.get("application/json; charset=utf-8")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (!response.isSuccessful()) {
                Logger.log(Level.WARNING, String.format("Lambda %s: request = %s respose = %s",
                    lambda.getLambdaID(), path, response.message()));
                return response.message();
            } else if (payload.length > 0 && parseJSON(new String(payload)).get("act").equals("add_isolate")) {
                int isolate = (int) parseJSON(responseBody).get("isolate");
                waitForIsolate(isolate, timeout);
            }
            return responseBody;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String sendRequest(String path, byte[] payload, Lambda lambda, long timeout) {
        return sendRequest(path, payload, timeout);
    }

    @Override
    public void close() {
        // TODO - client close?
    }
}