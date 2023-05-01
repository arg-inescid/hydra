package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;
import org.graalvm.argo.lambda_manager.core.Function;

public abstract class StartGraalvisor extends StartLambda {

    public StartGraalvisor(Lambda lambda, Function function) {
        super(lambda, function);
    }

    @Override
    protected OnProcessFinishCallback callback() {
        return new OnProcessFinishCallback() {

			@Override
			public void finish(int exitCode) {
				lambda.resetRegisteredInLambda();
			}
		};
    }
}
