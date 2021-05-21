package com.lambda_manager.core;

import com.lambda_manager.collectors.function_storage.FunctionStorage;
import com.lambda_manager.connectivity.client.LambdaManagerClient;
import com.lambda_manager.encoders.Encoder;
import com.lambda_manager.function_writer.FunctionWriter;
import com.lambda_manager.optimizers.Optimizer;
import com.lambda_manager.schedulers.Scheduler;

public class Configuration {

    private static boolean initialized = false;

    public static Scheduler scheduler;
    public static Optimizer optimizer;
    public static Encoder encoder;
    public static FunctionStorage storage;
    public static LambdaManagerClient client;
    public static FunctionWriter functionWriter;
    public static ArgumentStorage argumentStorage;

    private Configuration() {
    }

    public static void initFields(Scheduler scheduler, Optimizer optimizer, Encoder encoder,
                                  FunctionStorage storage, LambdaManagerClient client,
                                  FunctionWriter functionWriter, ArgumentStorage argumentStorage) {
        Configuration.initialized = true;
        Configuration.scheduler = scheduler;
        Configuration.optimizer = optimizer;
        Configuration.encoder = encoder;
        Configuration.storage = storage;
        Configuration.client = client;
        Configuration.functionWriter = functionWriter;
        Configuration.argumentStorage = argumentStorage;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
