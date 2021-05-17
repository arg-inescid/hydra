package com.lambda_manager.core;

import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.utils.logger.Logger;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;

import javax.inject.Singleton;
import java.util.logging.Level;

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
        try {
            Environment.setShutdownHookActive(true);
            LambdaManagerConfiguration configuration = LambdaManager.getConfiguration();
            if (configuration != null) {
                configuration.argumentStorage.prepareHandler();
                removeTapsFromPool(configuration);
                removeTapsOutsidePool(configuration);
                configuration.argumentStorage.cleanupStorage();
            }
        } catch (InterruptedException interruptedException) {
            Logger.log(Level.WARNING, Messages.ERROR_TAP_REMOVAL, interruptedException);
        } finally {
            Logger.close();
        }
    }
}
