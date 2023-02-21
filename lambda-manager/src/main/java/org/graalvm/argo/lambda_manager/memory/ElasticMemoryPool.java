package org.graalvm.argo.lambda_manager.memory;

public class ElasticMemoryPool extends MemoryPool {
	
	/**
	 * Amount of memory to allocate or deallocate at a time.
	 */
	private static long LOCAL_POOL_SIZE = 1024;
	
	/**
	 * Memory pool where the current pool can borrow more memory from.
	 */
	private MemoryPool parentPool;
	
	public ElasticMemoryPool(MemoryPool parentPool) {
		super(0 ,0);
		this.parentPool = parentPool;
	}
	
	@Override
    public synchronized boolean allocateMemoryLambda(long delta) {
        if (freeMemory >= delta) {
            freeMemory -= delta;
            return true;
        } else {
        	synchronized (parentPool) {
        		// We always allocate a multiple of LOCAL_POOL_SIZE blocks.
                long increment = (delta / LOCAL_POOL_SIZE + 1) * LOCAL_POOL_SIZE;
        		if (parentPool.allocateMemoryLambda(increment)) {
        			freeMemory = freeMemory + increment - delta;
        			maxMemory += increment;
        			return true;
            	} else {
            		return false;
            	}
			}
        }
    }

	@Override
    public synchronized boolean deallocateMemoryLambda(long delta) {
        freeMemory += delta;
        if (freeMemory > LOCAL_POOL_SIZE) {
            synchronized (parentPool) {
                if (freeMemory > LOCAL_POOL_SIZE) {
                    parentPool.deallocateMemoryLambda(LOCAL_POOL_SIZE);
                    freeMemory -= LOCAL_POOL_SIZE;
                    maxMemory -= LOCAL_POOL_SIZE;
                }
            }
        }
        return true;
    }

	@Override
	public long getMaxMemory() {
		return maxMemory;
	}
}
