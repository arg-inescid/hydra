package com.lambda_manager.core;

import com.lambda_manager.client.LambdaManagerClient;
import com.lambda_manager.encoders.Coder;
import com.lambda_manager.function_storage.FunctionStorage;
import com.lambda_manager.optimizers.FunctionOptimizer;
import com.lambda_manager.schedulers.Scheduler;

public class Configuration {

    private static boolean initialized = false;

    public static Scheduler scheduler;
    public static FunctionOptimizer optimizer;
    public static Coder coder;
    public static FunctionStorage storage;
    public static LambdaManagerClient client;
    public static ArgumentStorage argumentStorage;

    private Configuration() {
    }

    public static void initFields(Scheduler scheduler, FunctionOptimizer optimizer, Coder encoder,
                    FunctionStorage storage, LambdaManagerClient client,
                    ArgumentStorage argumentStorage) {
        Configuration.initialized = true;
        Configuration.scheduler = scheduler;
        Configuration.optimizer = optimizer;
        Configuration.coder = encoder;
        Configuration.storage = storage;
        Configuration.client = client;
        Configuration.argumentStorage = argumentStorage;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
