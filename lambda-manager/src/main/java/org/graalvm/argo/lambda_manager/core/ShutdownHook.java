package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTapsFromPool;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTapsOutsidePool;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;

import javax.inject.Singleton;

import java.util.Map;
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

    private void shutdownLambdas() {
        Map<String, Function> functionsMap = Configuration.storage.getAll();
        for (Function function : functionsMap.values()) {
            synchronized (function) {
                // First, move all lambdas to the idle list, ignoring that some requests might be still running.
                function.getIdleLambdas().addAll(function.getRunningLambdas());
                // Clear running lambdas.
                function.getRunningLambdas().clear();
            }
            // Then, for each idle lambda, force a shutdown.
            while(!function.getIdleLambdas().isEmpty()) {
                Lambda lambda = function.getIdleLambdas().get(0);
                new DefaultLambdaShutdownHandler(lambda, function).run();
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        try {
            Environment.setShutdownHookActive(true);
            if (Configuration.isInitialized()) {
                Configuration.argumentStorage.prepareHandler();
                shutdownLambdas();
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
