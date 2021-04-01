package com.lambda_manager.collectors.lambda_storage.impl;

import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.collectors.lambda_storage.LambdaStorage;
import com.lambda_manager.core.LambdaManager;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.utils.Tuple;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class DefaultLambdaStorage implements LambdaStorage {

    private final ConcurrentHashMap<String, LambdaInstancesInfo> lambdas = new ConcurrentHashMap<>();

    @Override
    public LambdaInstancesInfo register(String lambdaName) {
        LambdaInstancesInfo lambdaInstancesInfo = lambdas.get(lambdaName);
        if(lambdaInstancesInfo == null) {
            lambdaInstancesInfo = new LambdaInstancesInfo(lambdaName);
            lambdas.put(lambdaName, lambdaInstancesInfo);
        }
        return lambdaInstancesInfo;
    }

    @Override
    public void unregister(String lambdaName) {
        try {
            LambdaInstancesInfo removedLambda = lambdas.remove(lambdaName);
            // If we try to unregister unregistered lambda.
            if (removedLambda != null) {
                LambdaManagerConfiguration configuration = LambdaManager.getConfiguration();
                ProcessBuilder process = Processes.REMOVE_TAPS.build(new Tuple<>(removedLambda, null), configuration);
                process.start();
                process.join();
            }
        } catch (InterruptedException interruptedException) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Error during cleaning taps!",
                    interruptedException);
        }
    }

    @Override
    public LambdaInstancesInfo get(String lambdaName) {
        return lambdas.get(lambdaName);
    }

    @Override
    public Map<String, LambdaInstancesInfo> getAll() {
        return lambdas;
    }
}
