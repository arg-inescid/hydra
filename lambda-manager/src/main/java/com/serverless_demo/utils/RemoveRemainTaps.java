package com.serverless_demo.utils;

import com.serverless_demo.core.LambdaManager;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.processes.ProcessBuilder;
import com.serverless_demo.processes.Processes;
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
