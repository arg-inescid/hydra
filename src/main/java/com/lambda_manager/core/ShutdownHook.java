package com.lambda_manager.core;

import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.taps.RemoveTapsFromPool;
import com.lambda_manager.processes.taps.RemoveTapsOutsidePool;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.utils.logger.Logger;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;

import javax.inject.Singleton;
import java.util.logging.Level;

@SuppressWarnings("unused")
@Singleton
public class ShutdownHook implements ApplicationEventListener<ApplicationShutdownEvent> {

    private void removeTapsFromPool() throws InterruptedException {
        ProcessBuilder removeTapsWorker = new RemoveTapsFromPool().build();
        removeTapsWorker.start();
        removeTapsWorker.join();
    }

    private void removeTapsOutsidePool() throws InterruptedException {
        ProcessBuilder removeTapsOutsidePoolWorker = new RemoveTapsOutsidePool().build();
        removeTapsOutsidePoolWorker.start();
        removeTapsOutsidePoolWorker.join();
    }

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        try {
            Environment.setShutdownHookActive(true);
            if (Configuration.isInitialized()) {
                Configuration.argumentStorage.prepareHandler();
                removeTapsFromPool();
                removeTapsOutsidePool();
                Configuration.argumentStorage.cleanupStorage();
            }
        } catch (InterruptedException interruptedException) {
            Logger.log(Level.WARNING, Messages.ERROR_TAP_REMOVAL, interruptedException);
        } finally {
            Logger.close();
        }
    }
}
