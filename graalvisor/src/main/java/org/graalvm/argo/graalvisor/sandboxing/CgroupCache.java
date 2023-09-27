package org.graalvm.argo.graalvisor.sandboxing;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CgroupCache {

    private ConcurrentMap<Integer, CopyOnWriteArrayList<String>> cgroupCache = new ConcurrentHashMap<>();

    public CgroupCache() {
        warmupCgroupCache();
    }

    public String getCgroupIdByQuota(int quota) {
        String cgroupId;
        if (!cgroupCache.containsKey(quota)) {
            System.out.printf("New cache entry for %d CPU quota%n", quota);
            cgroupCache.put(quota, new CopyOnWriteArrayList<>());
        }

        if (cgroupCache.get(quota).isEmpty()) {
            cgroupId = createCgroup(quota);
        } else {
            cgroupId = cgroupCache.get(quota).get(0);
            System.out.printf("Using existing cgroup %s%n", cgroupId);
        }

        return cgroupId;
    }

    public void insertThreadInCgroup(String cgroupId, int quota) {
        long start = System.nanoTime();
        int threadId = NativeSandboxInterface.getThreadId();
        NativeSandboxInterface.insertThreadInCgroup(cgroupId, String.valueOf(threadId));
        cgroupCache.get(quota).remove(cgroupId);
        long finish = System.nanoTime();
        System.out.printf("Inserted %s in %s in %s us%n", threadId, cgroupId, (finish - start) / 1000);
    }

    public void removeThreadFromCgroup(String cgroupId, int quota) {
        long start = System.nanoTime();
        int threadId = NativeSandboxInterface.getThreadId();
        NativeSandboxInterface.removeThreadFromCgroup(cgroupId);
        cgroupCache.get(quota).add(cgroupId);
        long finish = System.nanoTime();
        System.out.printf("Removed %s from %s in %s us%n", threadId, cgroupId, (finish - start) / 1000);
    }

    public void warmupCgroupCache() {
        long start = System.nanoTime();
        System.out.println("Cgroup cache warmup...");
        cgroupCache.put(10000, new CopyOnWriteArrayList<>());
        createCgroup(10000);
        cgroupCache.put(100000, new CopyOnWriteArrayList<>());
        createCgroup(100000);
        long finish = System.nanoTime();
        System.out.printf("Cgroup cache warmup ended in %s us%n", (finish - start) / 1000);
    }

    private String createCgroup(int quota) {
        long start = System.nanoTime();
        String cgroupId = "cgroup-" + quota + "-" + UUID.randomUUID();
        NativeSandboxInterface.createCgroup(cgroupId);
        NativeSandboxInterface.setCgroupQuota(cgroupId, quota);
        cgroupCache.get(quota).add(cgroupId);
        long finish = System.nanoTime();
        System.out.printf("New %s in %s us %n", cgroupId, (finish - start) / 1000);

        return cgroupId;
    }
}
