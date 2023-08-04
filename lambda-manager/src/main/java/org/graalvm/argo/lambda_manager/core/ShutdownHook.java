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
        for (Lambda lambda : LambdaManager.lambdas) {
            new DefaultLambdaShutdownHandler(lambda).run();
        }
        for (Map<String, Lambda> map : LambdaManager.startingLambdas.values()) {
            for (Lambda lambda : map.values()) {
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
                Configuration.argumentStorage.getLambdaPool().tearDown();
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
