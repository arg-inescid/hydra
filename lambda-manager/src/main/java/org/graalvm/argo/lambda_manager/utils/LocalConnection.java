package org.graalvm.argo.lambda_manager.utils;

import java.io.File;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    protected final Lambda lambda;

    // Http client that uses unix domain sockets underneath.
    protected OkHttpClient ghClient;

    // Http clients for specific endpoints (functions).
    private Map<String, OkHttpClient> epClients;

    public LocalConnection(Lambda lambda) {
        this.lambda = lambda;
        this.epClients = new ConcurrentHashMap<>();
    }

    private void printError(String path, String payload, String message) {
        Logger.log(Level.WARNING, String.format("Lambda %s: request = %s payload = %s respose = %s",
            lambda.getLambdaID(), path, payload, message));
    }

    public String udsPath(String ep) {
        return "%s/%s/ep-%s.uds".formatted(Environment.CODEBASE, lambda.getLambdaName(), ep);
    }

    public String udsPath() {
        return Environment.CODEBASE + "/" + lambda.getLambdaName() + "/lambda.uds";
    }

    protected boolean waitForUDS(String path) {
        // Wait at least 20 times (100 ms), i.e., 2 seconds.
        for (int attempts = 0; attempts < 20; attempts++) {
            if (Files.notExists(Path.of(path))) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }
        return Files.exists(Path.of(path));
    }

    protected OkHttpClient getClient(String path) {
        try {
            SocketAddress addr = AFUNIXSocketAddress.of(new File(path));
            return new OkHttpClient.Builder()
                .socketFactory(new AFSocketFactory.FixedAddressSocketFactory(addr))
                .callTimeout(Duration.ofMinutes(1))
                .build();
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }
    }

    // TODO - this could be the uds existing. Then, we could have another (responsive/ping  that actually does this.)
    public boolean waitUntilReady() {
        // If the file does not exist, quit.
        if (!waitForUDS(udsPath())) {
            return false;
        }

        this.ghClient = getClient(udsPath());
        if (this.ghClient == null) {
            return false;
        }

        // Sending a simple list command to check if the server is up.
        sendRequest("command", "{ \"act\":\"list_ep\" }".getBytes(), null, 1);
        return true;
     }

    protected HashMap<?, ?> parseJSON(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, HashMap.class);
        } catch (Exception e) {
            Logger.log(Level.WARNING, String.format("Could not parse json in lambda %s: %s", lambda.getLambdaID(), json));
            return null;
        }
    }

    private void addEPClient(String path, String payload, String ep) {
        if (!waitForUDS(udsPath(ep))) {
            printError(path, payload, "Failed to find %s.".formatted(udsPath(ep)));
            return;
        }

        OkHttpClient epClient = getClient(udsPath(ep));

        if (epClient == null) {
            printError(path, payload, "Failed to find endpoint client for ep %s.".formatted(ep));
            return;
        }

        epClients.put(ep, epClient);
    }

    private boolean isInvocation(String path, String payload) {
        return path.equals("invoke");
    }

    private boolean isRegistration(String path, String payload) {
        return path.equals("command") && payload.contains("add_ep");
    }

    private String sendRequest(String path, String payload, long timeout) {
        String port = "80";
        OkHttpClient epClient = ghClient;
        String ep = String.valueOf(parseJSON(payload).get("ep"));

        // For invocations, we use the function-specific ep.
        if (isInvocation(path, payload)) {
            port = "8080";
            epClient = epClients.get(ep);
            if (epClient == null) {
                String message = "Unknown endpoint.";
                printError(path, payload, message);
                return message;
            }
        }

        Request request = new Request.Builder()
            .url(String.format("http://localhost:%s/%s", port, path))
            .post(RequestBody.create(payload, MediaType.get("application/json; charset=utf-8")))
            .build();

        try (Response response = epClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            if (response.isSuccessful()) {
                // When registering a new function, we need to add a new ep client for the function uds.
                if (isRegistration(path, payload)) {
                    addEPClient(path, payload, ep);
                }
                return responseBody;
            } else {
                printError(path, payload, response.message());
                return response.message();
            }
        } catch (SocketTimeoutException e) {
            String message = "timeout exception";
            printError(path, payload, message);
            return message;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
    }

    @Override
    public String sendRequest(String path, byte[] payload, Lambda lambda, long timeout) {
        return sendRequest(path, new String(payload), timeout);
    }

    @Override
    public void close() {
        // TODO - client close?
    }
}