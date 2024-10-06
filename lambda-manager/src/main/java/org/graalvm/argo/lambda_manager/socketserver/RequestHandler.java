package org.graalvm.argo.lambda_manager.socketserver;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RequestHandler {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static void processPayload(int requestId, String payload, SocketChannel client) {
        executor.execute(new RequestTask(requestId, payload, client));
    }

    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
