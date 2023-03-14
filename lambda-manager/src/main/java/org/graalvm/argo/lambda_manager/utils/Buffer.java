package org.graalvm.argo.lambda_manager.utils;

import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import java.util.Arrays;
import java.util.LongSummaryStatistics;
import java.util.logging.Level;

public class Buffer {

    private static final int CAPACITY = 1000000;

    private final long[] buffer;
    private int end;
    
    private long failedOffers;

    private Buffer() {
        this.buffer = new long[CAPACITY];
        this.end = -1;
        this.failedOffers = 0;
    }

    public static Buffer create() {
        return new Buffer();
    }

    public synchronized boolean offer(long value) {
        boolean isFull = end + 1 == CAPACITY;
        if (!isFull) {
            buffer[++end] = value;
            return true;
        }
        ++failedOffers;
        return false;
    }

    public synchronized long max() {
        if (end == -1) {
            // If the buffer is empty, we should return 0
            return 0;
        }
        LongSummaryStatistics stat = Arrays.stream(buffer, 0, end + 1).summaryStatistics();
        return stat.getMax();
    }

    public synchronized double avg() {
        LongSummaryStatistics stat = Arrays.stream(buffer, 0, end + 1).summaryStatistics();
        return stat.getAverage();
    }

    public synchronized void reset() {
        if (failedOffers > (CAPACITY / 2)) {
            Logger.log(Level.WARNING, String.format(Messages.WARNING_SMALL_BUFFER, failedOffers));
        }
        end = -1;
        failedOffers = 0;
    }

    public synchronized int size() {
        return end + 1;
    }
}
