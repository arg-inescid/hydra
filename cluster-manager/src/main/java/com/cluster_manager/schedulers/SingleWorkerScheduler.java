package com.cluster_manager.schedulers;

import com.cluster_manager.ClusterManager;
import com.cluster_manager.core.WorkerManager;

public class SingleWorkerScheduler implements Scheduler {

	@Override
	public WorkerManager schedule(String username, String functionName) throws Exception {
		return ClusterManager.workers.iterator().next();
	}

}
