package com.reflection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Reflection {

    private static String classNameArgument;

    public static void main(String[] args) throws Exception {
        System.out.println("VMM boot time: " + (System.currentTimeMillis() - Long.parseLong(args[args.length - 1])));
        HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(args[0])), 0);
        classNameArgument = args[1];
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = "Success";

            try {
                Class.forName(classNameArgument);
            } catch (ClassNotFoundException e) {
                response = "Failed";
            }

            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}

class DummyClass {
}
