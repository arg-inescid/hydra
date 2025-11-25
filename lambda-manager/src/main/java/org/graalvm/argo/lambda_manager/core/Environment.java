package org.graalvm.argo.lambda_manager.core;

import java.nio.file.Paths;

public class Environment {

    private Environment() { }

    private static long NEXT_ID = 0;

    private static volatile boolean shutdownHookActive = false;

    public static final int RAND_STRING_LEN = 10;

    // Tap name is limited to 15 characters. In our case tap names are created from prefix (4 chars) + random string (10 chars).
    public static final String TAP_PREFIX = "lmt";

    // Project Directories.
    public static final String CODEBASE = "codebase";
    public static final String MANAGER_LOGS = "manager_logs";
    public static final String LAMBDA_LOGS = "lambda_logs";
    public static final String MANAGER_METRICS = "manager_metrics";

    // Filenames.
    public static final String DEFAULT_FILENAME = "default_filename.log";
    public static final String OUTPUT = "output.log";
    public static final String ERROR = "error.log";
    public static final String MEMORY = "memory.log";
    public static final String MANAGER_LOG_FILENAME = Paths.get(MANAGER_LOGS, "lambda_manager.log").toString();
    public static final String CREATE_TAPS_FILENAME = Paths.get(MANAGER_LOGS, "create_taps.log").toString();
    public static final String REMOVE_TAPS_FILENAME = Paths.get(MANAGER_LOGS, "remove_taps.log").toString();
    public static final String PREPARE_DEVMAPPER_BASE_FILENAME = Paths.get(MANAGER_LOGS, "prepare_devmapper_base.log").toString();
    public static final String DELETE_DEVMAPPER_BASE_FILENAME = Paths.get(MANAGER_LOGS, "delete_devmapper_base.log").toString();

    public static final String MANAGER_METRICS_FILENAME = Paths.get(MANAGER_METRICS, "metrics.log").toString();

    // Runtime identifiers.
    public static final String GRAALVISOR_RUNTIME = "graalvisor";
    public static final String KNATIVE_RUNTIME = "knative";
    public static final String GRAALOS_RUNTIME = "graalos";
    public static final String FAASTION_RUNTIME = "faastion";
    public static final String FAASTION_OW_RUNTIME = "faastion-openwhisk";
    public static final String FAASTION_KN_RUNTIME = "faastion-knative";

    // Cold start sliding window parameters.
    /**
     * Minimum number of cold starts within a period.
     */
    public static final int AOT_OPTIMIZATION_THRESHOLD = 3;

    /**
     * Period during which we count number of cold starts (in ms).
     */
    public static final int SLIDING_WINDOW_PERIOD = 480000;

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
