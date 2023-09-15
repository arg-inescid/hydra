package org.graalvm.argo.lambda_manager.optimizers;

import org.graalvm.argo.lambda_manager.utils.RingBuffer;

public class ColdStartSlidingWindow {

    private final int period;
    private final RingBuffer buffer;

    public ColdStartSlidingWindow(int capacity, int period) {
        this.period = period;
        this.buffer = new RingBuffer(capacity);
    }

    public void addColdStart(long timestamp) {
        buffer.offer(timestamp);
    }

    public boolean worthOptimizing(long currentTimestamp) {
        long oldestTimestamp = buffer.readOldest();
        if (oldestTimestamp >= (currentTimestamp - period)) {
            buffer.reset();
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}
