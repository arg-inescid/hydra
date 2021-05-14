package com.lambda_manager.core;

import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.utils.Messages;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;

import javax.inject.Singleton;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@Singleton
public class ShutdownHook implements ApplicationEventListener<ApplicationShutdownEvent> {

    private void removeTapsFromPool(LambdaManagerConfiguration configuration) throws InterruptedException {
        ProcessBuilder removeTapsWorker = Processes.REMOVE_TAPS_FROM_POOL.build(null, configuration);
        removeTapsWorker.start();
        removeTapsWorker.join();
    }

    private void removeTapsOutsidePool(LambdaManagerConfiguration configuration) throws InterruptedException {
        ProcessBuilder removeTapsOutsidePoolWorker = Processes.REMOVE_TAPS_OUTSIDE_POOL.build(null, configuration);
        removeTapsOutsidePoolWorker.start();
        removeTapsOutsidePoolWorker.join();
    }

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        try {
            LambdaManagerConfiguration configuration = LambdaManager.getConfiguration();
            if (configuration != null) {
                removeTapsFromPool(configuration);
                removeTapsOutsidePool(configuration);
                configuration.argumentStorage.cleanupStorage();
            }
            logger.log(Level.INFO, Messages.EXECUTION_SUCCESS);
        } catch (InterruptedException interruptedException) {
            logger.log(Level.WARNING, Messages.ERROR_TAP_REMOVAL, interruptedException);
        }
    }
}
