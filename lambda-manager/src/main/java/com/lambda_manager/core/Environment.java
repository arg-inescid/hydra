package com.lambda_manager.core;

import java.nio.file.Paths;

public class Environment {

    private Environment() {
    }

    private static long NEXT_ID = 0;

    private static boolean shutdownHookActive = false;

    public static final int RAND_STRING_LEN = 10;
    public static final int IS_ALIVE_PAUSE = 50;

    // Time to wait until a new Lambda can be started.
    // TODO - this limit it a bit sensitive. It would be good if we could automatically estimate it.
    public static final int LAMBDA_STARTUP_THRESHOLD = 1000;

    // Tap name is limited to 15 characters. In our case tap names are created from prefix (4 chars) + random string (10 chars).
    public static final String TAP_PREFIX = "lmt";

    // Project Directories.
    public static final String CODEBASE = "codebase";
    public static final String MANAGER_LOGS = "manager_logs";
    public static final String LAMBDA_LOGS = "lambda_logs";

    // Lambda log directories.
    public static final String HOTSPOT = "pid_%d_hotspot";
    public static final String HOTSPOT_W_AGENT = "pid_%d_hotspot_w_agent";
    public static final String BUILD_VMM = "build_vmm";
    public static final String VMM = "pid_%d_vmm";

    // Filenames.
    public static final String DEFAULT_FILENAME = "default_filename.log";
    public static final String OUTPUT = "output.log";
    public static final String MEMORY = "memory.log";
    public static final String RUN_LOG = Paths.get("shared", "run.log").toString();
    public static final String MANAGER_LOG_FILENAME = Paths.get(MANAGER_LOGS, "lambda_manager.log").toString();
    public static final String CREATE_TAPS_FILENAME = Paths.get(MANAGER_LOGS, "create_taps.log").toString();
    public static final String REMOVE_TAPS_FILENAME = Paths.get(MANAGER_LOGS, "remove_taps.log").toString();

    public synchronized static long pid() {
        return NEXT_ID++;
    }

    public static boolean notShutdownHookActive() {
        return !shutdownHookActive;
    }

    public static void setShutdownHookActive(boolean shutdownHookActive) {
        Environment.shutdownHookActive = shutdownHookActive;
    }
}
