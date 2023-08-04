package org.graalvm.argo.lambda_manager.core;

import java.nio.file.Paths;

public class Environment {

    private Environment() { }

    private static long NEXT_ID = 0;

    private static volatile boolean shutdownHookActive = false;

    public static final int RAND_STRING_LEN = 10;

    // Time to wait until a new Lambda can be started.
    public static final int LAMBDA_STARTUP_THRESHOLD = 1000;

    // Maximum number of open requests that we will accept to send another request.
    public static final int LAMBDA_MAX_OPEN_REQ_COUNT = 1000;

    // Tap name is limited to 15 characters. In our case tap names are created from prefix (4 chars) + random string (10 chars).
    public static final String TAP_PREFIX = "lmt";

    // Project Directories.
    public static final String CODEBASE = "codebase";
    public static final String MANAGER_LOGS = "manager_logs";
    public static final String LAMBDA_LOGS = "lambda_logs";

    // Filenames.
    public static final String DEFAULT_FILENAME = "default_filename.log";
    public static final String OUTPUT = "output.log";
    public static final String MEMORY = "memory.log";
    public static final String MANAGER_LOG_FILENAME = Paths.get(MANAGER_LOGS, "lambda_manager.log").toString();
    public static final String CREATE_TAPS_FILENAME = Paths.get(MANAGER_LOGS, "create_taps.log").toString();
    public static final String REMOVE_TAPS_FILENAME = Paths.get(MANAGER_LOGS, "remove_taps.log").toString();
    public static final String PREPARE_DEVMAPPER_BASE_FILENAME = Paths.get(MANAGER_LOGS, "prepare_devmapper_base.log").toString();
    public static final String DELETE_DEVMAPPER_BASE_FILENAME = Paths.get(MANAGER_LOGS, "delete_devmapper_base.log").toString();

    // Graalvisor runtime identifiers.
    public static final String GRAALVISOR_RUNTIME = "graalvisor";
    public static final String GRAALVISOR_DOCKER_RUNTIME = "docker.io/sergiyivan/large-scale-experiment:graalvisor";
    public static final String HOTSPOT_DOCKER_RUNTIME = "docker.io/sergiyivan/large-scale-experiment:argo-hotspot";
    public static final String HOTSPOT_AGENT_DOCKER_RUNTIME = "docker.io/sergiyivan/large-scale-experiment:argo-hotspot-agent";

    public static final String OPENWHISK_DOCKER_RUNTIME = "docker.io/openwhisk/java8action:latest";

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
