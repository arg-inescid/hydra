package org.graalvm.argo.lambda_manager.core;

import org.graalvm.argo.lambda_manager.optimizers.LambdaExecutionMode;
import org.graalvm.argo.lambda_manager.processes.devmapper.DeleteDevmapperBase;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.lambda.DefaultLambdaShutdownHandler;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@SuppressWarnings("unused")
@Singleton
public class ShutdownHook implements ApplicationEventListener<ApplicationShutdownEvent> {

    private void deleteDevmapperBase() throws InterruptedException {
        ProcessBuilder deleteDevmapperBase = new DeleteDevmapperBase().build();
        deleteDevmapperBase.start();
        deleteDevmapperBase.join();
    }

    private void shutdownLambdas() {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        for (Lambda lambda : LambdaManager.lambdas) {
            executor.execute(new DefaultLambdaShutdownHandler(lambda)::run);
        }
        LambdaManager.lambdas.clear();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(600, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        try {
            Environment.setShutdownHookActive(true);
            // This sleep is used to allow other threads to react to the active shutdown hook flag.
            Thread.sleep(500);
            if (Configuration.isInitialized()) {
                shutdownLambdas();
                Configuration.argumentStorage.getLambdaPool().tearDown();
                Configuration.argumentStorage.tearDownMetricsScraper();
                LambdaType lambdaType = Configuration.argumentStorage.getLambdaType();
                if (lambdaType == LambdaType.VM_FIRECRACKER || lambdaType == LambdaType.VM_FIRECRACKER_SNAPSHOT) {
                    deleteDevmapperBase();
                }
            }
        } catch (InterruptedException interruptedException) {
            Logger.log(Level.WARNING, Messages.ERROR_TAP_REMOVAL, interruptedException);
        }
    }
}
