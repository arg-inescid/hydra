package org.graalvm.argo.lambda_manager.socketserver;

import org.graalvm.argo.lambda_manager.metrics.MetricsProvider;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class RequestTask implements Runnable {

    private final int requestId;
    private final String payload;
    private final ByteBuffer buffer;
    private final SocketChannel client;

    public RequestTask(int requestId, String payload, SocketChannel client) {
        this.requestId = requestId;
        this.payload = payload;
        this.client = client;
        this.buffer = ByteBuffer.allocate(256);
    }

    @Override
    public void run() {
        MetricsProvider.addConcurrentRequest();
        try {
            Map<String, String> parameters = RequestUtils.parsePayload(payload);
            String response = RequestUtils.processOperation(parameters);
            if (response.length() >= 248) {
                response = response.substring(0, 248);
            }
            SocketServer.writeResponse(buffer, requestId, response.getBytes(), client);
        } catch (Throwable t) {
            SocketServer.writeResponse(buffer, requestId, t.getMessage().getBytes(), client);
        } finally {
            MetricsProvider.removeConcurrentRequest();
        }
    }

}
