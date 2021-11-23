package org.graalvm.argo.cluster_manager.core;

import java.lang.reflect.Constructor;

import org.graalvm.argo.cluster_manager.function_storage.FunctionStorage;
import org.graalvm.argo.cluster_manager.schedulers.Scheduler;
import org.graalvm.argo.cluster_manager.utils.parser.ClusterManagerConfiguration;
import org.graalvm.argo.cluster_manager.utils.parser.ClusterManagerState;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.BeanContext;

public class ArgumentStorage {

    private ArgumentStorage() {
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ClusterManagerConfiguration parse(String configData) throws Exception {
    	return objectMapper.readValue(configData, ClusterManagerConfiguration.class);
    }
    
    private Object createObject(String className) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getConstructor();
        return constructor.newInstance();
    }

    private void prepareConfiguration(ClusterManagerState managerState, String workers)
                    throws Exception {
        Scheduler scheduler = (Scheduler) createObject(managerState.getScheduler());
        FunctionStorage storage = (FunctionStorage) createObject(managerState.getStorage());
        Configuration.initFields(scheduler, storage, workers, this);
    }

    public void doInitialize(ClusterManagerConfiguration managerConfiguration, BeanContext beanContext) throws Exception {
        prepareConfiguration(managerConfiguration.getManagerState(), managerConfiguration.getWorkers());
    }

    public static void initializeClusterManager(ClusterManagerConfiguration clusterManagerConfiguration, BeanContext beanContext) throws Exception {
        new ArgumentStorage().doInitialize(clusterManagerConfiguration, beanContext);
    }

}

