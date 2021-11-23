package org.graalvm.argo.cluster_manager.schedulers;

import org.graalvm.argo.cluster_manager.ClusterManager;
import org.graalvm.argo.cluster_manager.core.WorkerManager;

public class SingleWorkerScheduler implements Scheduler {

	@Override
	public WorkerManager schedule(String username, String functionName) throws Exception {
		return ClusterManager.workers.iterator().next();
	}

}
