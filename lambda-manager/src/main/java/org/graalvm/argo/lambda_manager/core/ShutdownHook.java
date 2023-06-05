package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTapsFromPool;
import org.graalvm.argo.lambda_manager.processes.taps.RemoveTapsOutsidePool;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;

import javax.inject.Singleton;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
        for (Lambda lambda : LambdaManager.lambdas) {
            new DefaultLambdaShutdownHandler(lambda).run();
        }
        for (Set<Lambda> set : LambdaManager.startingLambdas.values()) {
            for (Lambda lambda : set) {
                new DefaultLambdaShutdownHandler(lambda).run();
            }
        }
        LambdaManager.lambdas.clear();
        LambdaManager.startingLambdas.get(LambdaExecutionMode.HOTSPOT_W_AGENT).clear();
        LambdaManager.startingLambdas.get(LambdaExecutionMode.HOTSPOT).clear();
        LambdaManager.startingLambdas.get(LambdaExecutionMode.GRAALVISOR).clear();
        LambdaManager.startingLambdas.get(LambdaExecutionMode.CUSTOM).clear();
    }

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        try {
            Environment.setShutdownHookActive(true);
            // This sleep is used to allow other threads to react to the active shutdown hook flag.
            Thread.sleep(500);
            if (Configuration.isInitialized()) {
                shutdownLambdas();
                removeTapsFromPool();
                removeTapsOutsidePool();
                Configuration.argumentStorage.cleanupStorage();
            }
        } catch (InterruptedException interruptedException) {
            Logger.log(Level.WARNING, Messages.ERROR_TAP_REMOVAL, interruptedException);
        }
    }
}
