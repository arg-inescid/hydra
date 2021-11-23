package org.graalvm.argo.cluster_manager.schedulers;

import org.graalvm.argo.cluster_manager.core.WorkerManager;

/**
 * The scheduler decides where (in which WorkerManager) to execute a function invocation.
 */
public interface Scheduler {

	/**
	 * Returns the WorkerManger that should host the function invocation.
	 * @param username
	 * @param functionName
	 * @return
	 * @throws Exception
	 */
    WorkerManager schedule(String username, String functionName) throws Exception;
	
}
