package org.graalvm.argo.cluster_manager.core;

import org.graalvm.argo.cluster_manager.ClusterManager;
import org.graalvm.argo.cluster_manager.function_storage.FunctionStorage;
import org.graalvm.argo.cluster_manager.schedulers.Scheduler;

public class Configuration {

    private static boolean initialized = false;

    public static Scheduler scheduler;
    public static FunctionStorage storage;
    public static ArgumentStorage argumentStorage;

    private Configuration() {
    }

    public static void initFields(Scheduler scheduler, FunctionStorage storage, String workers, ArgumentStorage argumentStorage) {
        Configuration.initialized = true;
        Configuration.scheduler = scheduler;
        Configuration.storage = storage;
        Configuration.argumentStorage = argumentStorage;
        for (String workerAddress : workers.split(",")) {
        	ClusterManager.workers.add(new WorkerManager(workerAddress));
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}