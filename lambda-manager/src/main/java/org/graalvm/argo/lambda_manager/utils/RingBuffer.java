package org.graalvm.argo.lambda_manager.utils;

import java.util.Arrays;

/**
 * This data structure is not thread-safe; therefore, it relies on
 * synchronization provided by the caller.
 */
public class RingBuffer {

    private final long[] buffer;
    private volatile int writeSequence;
    private final int capacity;

    public RingBuffer(int capacity) {
        this.buffer = new long[capacity];
        this.capacity = capacity;
        this.writeSequence = -1;
    }

    public void offer(long element) {
        buffer[++writeSequence % capacity] = element;
    }

    public long readOldest() {
        return buffer[(writeSequence + 1) % capacity];
    }

    public void reset() {
        Arrays.fill(buffer, 0);
        writeSequence = -1;
    }

    @Override
    public String toString() {
        return Arrays.toString(buffer) + "; writeSequence = " + writeSequence;
    }

}
