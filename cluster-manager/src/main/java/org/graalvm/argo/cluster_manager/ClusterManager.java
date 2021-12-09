package org.graalvm.argo.cluster_manager;

import java.util.HashSet;
import java.util.Set;

import org.graalvm.argo.cluster_manager.core.ArgumentStorage;
import org.graalvm.argo.cluster_manager.core.Configuration;
import org.graalvm.argo.cluster_manager.core.WorkerManager;
import org.graalvm.argo.cluster_manager.utils.Messages;

import io.micronaut.context.BeanContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpParameters;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.RxHttpClient;

public class ClusterManager {

	private static final int WORKER_ATTEMPTS = 5;

	public static Set<WorkerManager> workers = new HashSet<>();

	public static String sendRequest(RxHttpClient workerClient, MutableHttpRequest<?> request, String errorMessage) {
		for (int attempt = 0; attempt < WORKER_ATTEMPTS; attempt++) {
            try {
                return workerClient.retrieve(request).blockingFirst();
            } catch (Exception e) {
                try {
                    e.printStackTrace();
                    Thread.sleep(500);
                } catch (InterruptedException interruptedException) {
                    // Skipping raised exception.
                }
            }
		}
        return errorMessage;
	}

	public static String processRequest(String username, String functionName, String arguments, String warmupCount, BeanContext beanContext) {
        if (!Configuration.isInitialized()) {
            System.err.println(Messages.NO_CONFIGURATION_UPLOADED);
            return Messages.NO_CONFIGURATION_UPLOADED;
        }

        try {
			WorkerManager worker = Configuration.scheduler.schedule(username, functionName);
			MutableHttpRequest<?> request = HttpRequest.POST(String.format("/%s/%s", username, functionName), arguments == null ? "" : arguments);
			MutableHttpParameters requestParameters = request.getParameters();
			if (warmupCount != null) {
				requestParameters.add("count", warmupCount);
			}
			return ClusterManager.sendRequest(worker.getClient(beanContext), request, String.format(Messages.INTERNAL_ERROR, functionName));
		} catch (Exception e) {
            System.err.println(e.getMessage());
            return Messages.INTERNAL_ERROR;
		}
	}
	public static String uploadFunction(int allocate, String username, String functionName, String functionLanguage, String functionEntryPoint, String arguments, byte[] functionCode, BeanContext beanContext) {
        if (!Configuration.isInitialized()) {
            System.err.println(Messages.NO_CONFIGURATION_UPLOADED);
            return Messages.NO_CONFIGURATION_UPLOADED;
        }

        try {
			Configuration.storage.register(allocate, username, functionName, functionLanguage, functionEntryPoint, arguments, functionCode, beanContext);
			return String.format(Messages.SUCCESS_FUNCTION_UPLOAD, functionName);
		} catch (Exception e) {
            System.err.println(e.getMessage());
            return Messages.INTERNAL_ERROR;
		}
	}

	public static String removeFunction(String username, String functionName, BeanContext beanContext) {
        if (!Configuration.isInitialized()) {
            System.err.println(Messages.NO_CONFIGURATION_UPLOADED);
            return Messages.NO_CONFIGURATION_UPLOADED;
        }

        try {
			Configuration.storage.unregister(username, functionName, beanContext);
			return String.format(Messages.SUCCESS_FUNCTION_REMOVE, functionName);
		} catch (Exception e) {
            System.err.println(e.getMessage());
            return Messages.INTERNAL_ERROR;
		}
	}

	public static String configureManager(String managerConfiguration, BeanContext beanContext) {
        String responseString;
        try {
            if (!Configuration.isInitialized()) {
                ArgumentStorage.initializeClusterManager(ArgumentStorage.parse(managerConfiguration), beanContext);
                System.out.println(Messages.SUCCESS_CONFIGURATION_UPLOAD);
                responseString = Messages.SUCCESS_CONFIGURATION_UPLOAD;
            } else {
                responseString = Messages.CONFIGURATION_ALREADY_UPLOADED;
            }
        } catch (Throwable t) {
        	System.err.println(t.getMessage());
            responseString = Messages.INTERNAL_ERROR;
        }
        return responseString;
	}

}
