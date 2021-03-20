package com.lambda_manager.utils;

import com.lambda_manager.core.LambdaManager;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;

import javax.inject.Singleton;

@SuppressWarnings("unused")
@Singleton
public class RemoveRemainTaps implements ApplicationEventListener<ApplicationShutdownEvent> {

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        try {
            LambdaManagerConfiguration configuration = LambdaManager.getLambdaManager().getConfiguration();
            if (configuration != null) {
                ProcessBuilder removeTapsWorker = Processes.REMOVE_TAPS.build(null, configuration);
                removeTapsWorker.start();
                removeTapsWorker.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
