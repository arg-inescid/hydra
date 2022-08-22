package org.graalvm.argo.lambda_manager.utils.logger;

import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.logging.Level;

public final class Logger {

    private static java.util.logging.Logger LOGGER;

    public static void log(Level level, String msg) {
        if (Environment.notShutdownHookActive()) {
            LOGGER.log(level, msg);
        } else if (isLoggable(level)) {
            System.out.println(msg);
        }
    }

    public static void log(Level level, String msg, Throwable throwable) {
        if (Environment.notShutdownHookActive()) {
            LOGGER.log(level, msg, throwable);
        } else if (isLoggable(level)) {
            System.out.println(msg);
        }
    }

    public static void setLogger(java.util.logging.Logger logger) {
        Logger.LOGGER = logger;
    }

    public static boolean isLoggable(Level level) {
        return LOGGER.isLoggable(level);
    }
}
