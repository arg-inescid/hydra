package com.lambda_manager.core;

import com.lambda_manager.code_writer.FunctionWriter;
import com.lambda_manager.collectors.lambda_storage.LambdaStorage;
import com.lambda_manager.connectivity.client.LambdaManagerClient;
import com.lambda_manager.encoders.Encoder;
import com.lambda_manager.optimizers.Optimizer;
import com.lambda_manager.schedulers.Scheduler;

public class LambdaManagerConfiguration {
    public final Scheduler scheduler;
    public final Optimizer optimizer;
    public final Encoder encoder;
    public final LambdaStorage storage;
    public final LambdaManagerClient client;
    public final FunctionWriter functionWriter;
    public final LambdaManagerArgumentStorage argumentStorage;

    public LambdaManagerConfiguration(Scheduler scheduler, Optimizer optimizer, Encoder encoder,
                                      LambdaStorage storage, LambdaManagerClient client,
                                      FunctionWriter functionWriter, LambdaManagerArgumentStorage argumentStorage) {
        this.scheduler = scheduler;
        this.optimizer = optimizer;
        this.encoder = encoder;
        this.storage = storage;
        this.client = client;
        this.functionWriter = functionWriter;
        this.argumentStorage = argumentStorage;
    }
}
