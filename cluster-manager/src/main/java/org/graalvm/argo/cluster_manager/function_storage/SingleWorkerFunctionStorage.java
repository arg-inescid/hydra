package org.graalvm.argo.cluster_manager.function_storage;

import org.graalvm.argo.cluster_manager.ClusterManager;
import org.graalvm.argo.cluster_manager.core.WorkerManager;
import org.graalvm.argo.cluster_manager.utils.Messages;

import io.micronaut.context.BeanContext;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpParameters;
import io.micronaut.http.MutableHttpRequest;

public class SingleWorkerFunctionStorage implements FunctionStorage {

	@Override
	public String register(int allocate, String username, String functionName, String functionLanguage,
			String functionEntryPoint, String arguments, byte[] functionCode, BeanContext beanContext) throws Exception {
		WorkerManager worker = ClusterManager.workers.iterator().next();
		MutableHttpRequest<?> request = HttpRequest.POST("/upload_function", functionCode).contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE);
		MutableHttpParameters requestParameters = request.getParameters();
		requestParameters.add("allocate", Integer.toString(allocate));
		requestParameters.add("username", username);
		requestParameters.add("function_name", functionName);
		requestParameters.add("function_language", functionLanguage);
		requestParameters.add("function_entry_point", functionEntryPoint);
		if (arguments != null) {
			requestParameters.add("arguments", arguments);
		}
		return ClusterManager.sendRequest(worker.getClient(beanContext), request, String.format(Messages.ERROR_FUNCTION_UPLOAD, functionName));
	}

	@Override
	public String unregister(String username, String functionName, BeanContext beanContext) throws Exception {
		WorkerManager worker = ClusterManager.workers.iterator().next();
		MutableHttpRequest<?> request = HttpRequest.POST("/remove_function", functionName);
		MutableHttpParameters requestParameters = request.getParameters();
		requestParameters.add("username", username);
		requestParameters.add("function_name", functionName);
		return ClusterManager.sendRequest(worker.getClient(beanContext), request, String.format(Messages.ERROR_FUNCTION_REMOVE, functionName));
	}

}
