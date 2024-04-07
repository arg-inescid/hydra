package org.graalvm.argo.lambda_manager.memory;

public abstract class MemoryPool {

	/**
     * Maximum total memory that the Lambda Manager is allowed to allocate.
     */
	protected long maxMemory;
    
    /**
     * Total memory that is still available to allocate.
     */
    protected long freeMemory;
	
    public MemoryPool(long maxMemory, long freeMemory) {
    	this.maxMemory = maxMemory;
    	this.freeMemory = freeMemory;
    }
    
	public abstract boolean allocateMemoryLambda(long delta);

    public abstract boolean deallocateMemoryLambda(long delta);
    
    public void reset() {
    	maxMemory = 0;
    	freeMemory = 0;
    }

    public long getMaxMemory() {
    	return maxMemory;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

}
