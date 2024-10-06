package org.graalvm.argo.lambda_manager.socketserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SocketServer {

    private static final String CLOSE_CONNECTION_MESSAGE = "CLOSE_CONNECTION";

    private final int port;

    // We need separate read/write buffers as we can write responses while reading requests.
    private static final ByteBuffer readBuffer = ByteBuffer.allocate(1024);

    public SocketServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress("localhost", port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                if (key.isAcceptable()) {
                    register(selector, serverSocket);
                }
                if (key.isReadable()) {
                    while (!read(key)) {}
                }
                iter.remove();
            }
        }
    }

    private boolean read(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        if (client.read(readBuffer) != -1) {
            // Prepare buffer.
            readBuffer.flip();
            // Real all requests.
            while (readBuffer.hasRemaining()) {
                // Making sure we have enough room to read the mandatory bytes.
                if (readBuffer.remaining() < 8) {
                    readBuffer.compact();
                    return false;
                }
                int requestId = readBuffer.getInt();
                int payloadLength = readBuffer.getInt();
                if (readBuffer.remaining() < payloadLength) {
                    // Roll back two integers that were already read.
                    readBuffer.position(readBuffer.position() - 4*2);
                    // Copy all unread bytes including two integers to the beginning, set position to continue writing.
                    readBuffer.compact();
                    return false;
                }
                byte[] payloadBytes = new byte[payloadLength];
                readBuffer.get(payloadBytes);
                String payload = new String(payloadBytes);
                if (CLOSE_CONNECTION_MESSAGE.equals(payload)) {
                    closeClientConnection(client);
                } else {
                    RequestHandler.processPayload(requestId, payload, client);
                }
            }
            readBuffer.clear();
        } else {
            closeClientConnection(client);
        }
        return true;
    }

    private void closeClientConnection(SocketChannel client) throws IOException {
        System.out.println("End-of-stream, closing connection...");
        client.close();
    }

    private void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    public static void writeResponse(ByteBuffer buffer, int requestId, byte[] responsePayload, SocketChannel client) {
        try {
            buffer.clear();
            buffer.putInt(requestId);
            buffer.putInt(responsePayload.length);
            buffer.put(responsePayload);
            buffer.flip();
            client.write(buffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            buffer.clear();
        }
    }
}
