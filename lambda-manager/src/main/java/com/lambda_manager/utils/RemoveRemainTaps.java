package com.lambda_manager.utils;

import com.lambda_manager.core.LambdaManager;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;

import javax.inject.Singleton;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@Singleton
public class RemoveRemainTaps implements ApplicationEventListener<ApplicationShutdownEvent> {

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        try {
            LambdaManagerConfiguration configuration = LambdaManager.getConfiguration();
            if (configuration != null) {
                ProcessBuilder removeTapsWorker = Processes.REMOVE_TAPS.build(null, configuration);
                removeTapsWorker.start();
                configuration.argumentStorage.cleanupStorage();
                removeTapsWorker.join();
            }
            logger.log(Level.INFO, "Execution finished successfully! :)");
        } catch (InterruptedException interruptedException) {
            logger.log(Level.WARNING, "Error during cleaning taps!", interruptedException);
        }
    }
}
