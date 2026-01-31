package org.graalvm.argo.lambda_manager.utils;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.core.Environment;
import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;

public class LocalConnection implements LambdaConnection {

    // File system path where the Unix Domain Socket is located.
    private final String udsPath;

    // Identifier of he lambda we are connecting.
    private final long lambdaID;

    // Open channel to read and write.
    private SocketChannel channel;

    // Buffer used read data into.
    private final ByteBuffer buffer;

    public LocalConnection(Lambda lambda) {
        this.udsPath = Environment.CODEBASE + "/" + lambda.getLambdaName() + "/lambda.uds";
        this.lambdaID = lambda.getLambdaID();
        Logger.log(Level.WARNING, "GRAALOS uds path is " + this.udsPath);
        this.buffer = ByteBuffer.allocate(4 * 1024);
    }

    private boolean waitForUDS() {
        // Wait at least 20 times (100 ms), i.e., 2 seconds.
        for (int attempts = 0; attempts < 20; attempts++) {
            if (Files.notExists(Path.of(this.udsPath))) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    continue;
                }
            }
        }

        return Files.exists(Path.of(this.udsPath));
    }

    // TODO - this could be the uds existing. Then, we could have another (responsive/ping  that actually does this.)
    public boolean waitUntilReady() {
        String payload = "{ \"act\":\"list_ep\" }";

        // If the file does not exist, quit.
        if (!waitForUDS()) {
            return false;
        }

        Logger.log(Level.WARNING, "GRAALOS wait until ready");
        try {
            channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            channel.connect(UnixDomainSocketAddress.of(udsPath));
            String response = sendRequest("/command", payload.getBytes(), null, 0); // TODO - fix.
            Logger.log(Level.WARNING, "GRAALOS wait until ready response = " + response);
            return true;
        } catch (Exception e) {
            Logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
     }

    public String sendRequest(String path, byte[] payload, Lambda lambda, long timeout) {
        String response;
        StringBuilder request = new StringBuilder();
        request.append("POST ").append(path).append(" HTTP/1.1\r\n");
        request.append("Host: localhost\r\n");
        request.append("User-Agent: LambdaManager\r\n");
        request.append("Accept: */*\r\n");
        request.append("Content-Length: ").append(payload.length).append("\r\n");
        request.append("Content-Type: application/json\r\n");
        request.append("\r\n");

        try {
            // Write Header.
            channel.write(ByteBuffer.wrap(request.toString().getBytes(StandardCharsets.UTF_8)));

            // Write payload.
            channel.write(ByteBuffer.wrap(payload));

            // Read Response.
            response = new String(buffer.array(), 0, channel.read(buffer));

            // Discard data.
            buffer.clear();
        } catch (IOException e) {
            Logger.log(Level.WARNING, "Received IOException in lambda " + this.lambdaID + ". Message: " + e.getMessage());
            response = e.getLocalizedMessage();
        }

        return response;
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}