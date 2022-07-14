package org.graalvm.argo.lambda_manager.client;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;


public interface LambdaManagerClient {

	String registerFunction(Lambda lambda, Function function);

	String deregisterFunction(Lambda lambda, Function function);

    String invokeFunction(Lambda lambda, Function function, String arguments);
}