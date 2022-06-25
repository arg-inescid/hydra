package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.client.LambdaManagerClient;
import org.graalvm.argo.lambda_manager.encoders.Coder;
import org.graalvm.argo.lambda_manager.function_storage.FunctionStorage;
import org.graalvm.argo.lambda_manager.schedulers.Scheduler;

public class Configuration {

    private static boolean initialized = false;

    /**
     * Number of times a request will be sent to a different Lambda upon timeout.
     */
    public static final int LAMBDA_FAULT_TOLERANCE = 3;

    /**
     * Number of times a request will be re-sent to a particular Lambda upon an error.
     */
    public static final int FAULT_TOLERANCE = 300;

    public static Scheduler scheduler;
    public static Coder coder;
    public static FunctionStorage storage;
    public static LambdaManagerClient client;
    public static ArgumentStorage argumentStorage;

    private Configuration() { }

    public static void initFields(Scheduler scheduler, Coder encoder,
                    FunctionStorage storage, LambdaManagerClient client,
                    ArgumentStorage argumentStorage) {
        Configuration.initialized = true;
        Configuration.scheduler = scheduler;
        Configuration.coder = encoder;
        Configuration.storage = storage;
        Configuration.client = client;
        Configuration.argumentStorage = argumentStorage;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
