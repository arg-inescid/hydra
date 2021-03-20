package com.serverless_demo.core;

import com.serverless_demo.code_writer.CodeWriter;
import com.serverless_demo.collectors.lambda_storage.LambdaStorage;
import com.serverless_demo.connectivity.client.LambdaManagerClient;
import com.serverless_demo.encoders.Encoder;
import com.serverless_demo.optimizers.Optimizer;
import com.serverless_demo.schedulers.Scheduler;
import com.serverless_demo.utils.LambdaManagerArgumentStorage;

public class LambdaManagerConfiguration {
    public final Scheduler scheduler;
    public final Optimizer optimizer;
    public final Encoder encoder;
    public final LambdaStorage storage;
    public final LambdaManagerClient client;
    public final CodeWriter codeWriter;
    public final LambdaManagerArgumentStorage argumentStorage;

    public LambdaManagerConfiguration(Scheduler scheduler, Optimizer optimizer, Encoder encoder,
                                      LambdaStorage storage, LambdaManagerClient client,
                                      CodeWriter codeWriter, LambdaManagerArgumentStorage argumentStorage) {
        this.scheduler = scheduler;
        this.optimizer = optimizer;
        this.encoder = encoder;
        this.storage = storage;
        this.client = client;
        this.codeWriter = codeWriter;
        this.argumentStorage = argumentStorage;
    }
}
