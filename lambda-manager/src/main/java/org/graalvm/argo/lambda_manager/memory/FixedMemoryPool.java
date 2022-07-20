package org.graalvm.argo.lambda_manager.memory;

public class FixedMemoryPool extends MemoryPool {

    public FixedMemoryPool(long maxMemory, long freeMemory) {
    	super(maxMemory, freeMemory);
    }

    public synchronized boolean allocateMemoryLambda(long delta) {
        if (freeMemory >= delta) {
            freeMemory -= delta;
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean deallocateMemoryLambda(long delta) {
        freeMemory += delta;
        return true;
    }
    
    public long getMaxMemory() {
    	return this.maxMemory;
    }
}
