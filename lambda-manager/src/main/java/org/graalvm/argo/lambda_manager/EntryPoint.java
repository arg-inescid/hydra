package org.graalvm.argo.lambda_manager;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanContext;
import io.micronaut.runtime.Micronaut;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.core.ShutdownHook;
import org.graalvm.argo.lambda_manager.core.StartupHook;
import org.graalvm.argo.lambda_manager.socketserver.SocketServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EntryPoint {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("The first argument should be a path to a configuration file.");
            System.exit(1);
        }
        String configPath = args[0];
        // Launch the Micronaut app and configure.
        ApplicationContext context = Micronaut.run(EntryPoint.class, args);
        configure(configPath, context.getBean(LambdaManagerController.class).beanContext);
        // If we want to use socket server, stop the Micronaut's HTTP server and run the socket server.
        if (args.length > 1 && "socket".equals(args[1])) {
            // context.getBean(io.micronaut.runtime.server.EmbeddedServer.class).stop();
//            context.getBean(io.micronaut.http.server.netty.NettyHttpServer.class).stop();
            SocketServer server = new SocketServer(30009);
            try {
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void configure(String configPath, BeanContext beanContext) {
        // No builtin startup hooks in Java, running it directly.
        new StartupHook().run();
        // Registering shutdown hook instead of using the Micronaut's lifecycle management.
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        // Configuring Lambda Manager with the JSON configuration.
        try {
            LambdaManager.configureManager(Files.readString(Paths.get(configPath)), beanContext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
