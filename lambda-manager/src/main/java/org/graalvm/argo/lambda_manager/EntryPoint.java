package org.graalvm.argo.lambda_manager;

import io.micronaut.runtime.Micronaut;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.graalvm.argo.lambda_manager.core.LambdaManager;
import org.graalvm.argo.lambda_manager.core.ShutdownHook;
import org.graalvm.argo.lambda_manager.core.StartupHook;
import org.graalvm.argo.lambda_manager.socketserver.SocketServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EntryPoint {

    public static void main(String[] args) {
        Options options = prepareOptions();
        try {
            CommandLine cmd = new DefaultParser().parse(options, args);
            String configPath = cmd.getOptionValue("config");
            String variablesPath = cmd.getOptionValue("variables");
            boolean httpServer = cmd.hasOption("http");
            boolean socketServer = cmd.hasOption("socket");

            if (httpServer) {
                Micronaut.run(EntryPoint.class, args);
            }

            configure(configPath);

            if (socketServer) {
                SocketServer server = new SocketServer(30009);
                server.start();
            }
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("utility-name", options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void configure(String configPath) {
        // No builtin startup hooks in Java, running it directly.
        new StartupHook().run();
        // Registering shutdown hook instead of using the Micronaut's lifecycle management.
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        // Configuring Lambda Manager with the JSON configuration.
        try {
            System.out.println(LambdaManager.configureManager(Files.readString(Paths.get(configPath))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Options prepareOptions() {
        Options options = new Options();
        Option config = new Option("c", "config", true, "Configuration JSON file path.");
        config.setRequired(true);
        options.addOption(config);
        Option variables = new Option("vars", "variables", true, "Variables JSON file path.");
        variables.setRequired(false);
        options.addOption(variables);
        Option http = new Option("h", "http", false, "Start an HTTP server.");
        http.setRequired(false);
        options.addOption(http);
        Option socket = new Option("s", "socket", false, "Start a socket server.");
        socket.setRequired(false);
        options.addOption(socket);
        return options;
    }

}
