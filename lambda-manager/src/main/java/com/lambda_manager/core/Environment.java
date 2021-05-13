package com.lambda_manager.core;

import java.nio.file.Paths;

public class Environment {

    private Environment() {
    }

    private static long NEXT_ID = 0;

    public static final int RAND_STRING_LEN = 10;
    public static final int IS_ALIVE_PAUSE = 50;
    public static final int THRESHOLD = 200;
    // Tap name is limited to 15 characters. In our case tap names are created from prefix (4 chars) + random string (10 chars).
    public static final String TAP_PREFIX = "lmt";

    public static final String HOTSPOT = "hotspot";
    public static final String HOTSPOT_W_AGENT = "hotspot_w_agent";
    public static final String NATIVE_IMAGE = "native_image";

    // Directories.
    public static final String CODEBASE = Paths.get("src", "codebase").toString();
    public static final String MANAGER_LOGS = Paths.get("src", "manager_logs").toString();
    public static final String LAMBDA_LOGS = Paths.get("src", "lambda_logs").toString();

    // Filenames.
    public static final String DEFAULT_FILENAME = "default_filename.log";
    public static final String RUN_LOG = Paths.get("shared", "run.log").toString();
    public static final String MANAGER_LOG_FILENAME = Paths.get(MANAGER_LOGS, "lambda_manager.log").toString();
    public static final String CREATE_TAPS_FILENAME = Paths.get(MANAGER_LOGS, "create_taps.log").toString();
    public static final String REMOVE_TAPS_FILENAME = Paths.get(MANAGER_LOGS, "remove_taps.log").toString();

    public static long pid() {
        return NEXT_ID++;
    }
}
