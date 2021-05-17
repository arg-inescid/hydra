package com.lambda_manager.utils.logger;

import com.lambda_manager.core.Environment;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public final class Logger {

    private static java.util.logging.Logger LOGGER;
    private static Handler HANDLER;

    public static void log(Level level, String msg) {
        if (!Environment.isShutdownHookActive()) {
            LOGGER.log(level, msg);
        } else {
            HANDLER.publish(new LogRecord(level, msg));
        }
    }

    public static void log(Level level, String msg, Throwable throwable) {
        if (!Environment.isShutdownHookActive()) {
            LOGGER.log(level, msg, throwable);
        } else {
            LogRecord record = new LogRecord(level, msg);
            record.setThrown(throwable);
            HANDLER.publish(record);
        }
    }

    public static void setLogger(java.util.logging.Logger logger) {
        Logger.LOGGER = logger;
    }

    public static void setHandler(Handler handler) {
        Logger.HANDLER = handler;
    }

    public static void close() {
        if (HANDLER != null) {
            HANDLER.close();
        }
    }
}
