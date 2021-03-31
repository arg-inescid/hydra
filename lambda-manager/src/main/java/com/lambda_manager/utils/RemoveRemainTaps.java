package com.lambda_manager.utils;

import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManager;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
@Singleton
public class RemoveRemainTaps implements ApplicationEventListener<ApplicationShutdownEvent> {

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        try {
            LambdaManagerConfiguration configuration = LambdaManager.getLambdaManager().getConfiguration();
            if (configuration != null) {
                List<ProcessBuilder> removeTapsWorkers = new ArrayList<>();
                ProcessBuilder removeTapsWorker;
                for(LambdaInstancesInfo lambda : configuration.storage.getAll().values()) {
                    removeTapsWorker = Processes.REMOVE_TAPS.build(new Tuple<>(lambda, null), null);
                    removeTapsWorker.start();
                    removeTapsWorkers.add(removeTapsWorker);
                }
                for (ProcessBuilder worker : removeTapsWorkers) {
                    worker.join();
                }
            }
        } catch (InterruptedException interruptedException) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Error during cleaning taps!",
                    interruptedException);
        }
    }
}
