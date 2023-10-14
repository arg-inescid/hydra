package org.graalvm.argo.graalvisor.sandboxing;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CgroupCache {
    static class ShutDownHook extends Thread {
        public void run() {
            System.out.println("Shutting down CgroupCache (hook)...");
            try {
                deleteMainCgroup();
            } catch (Exception e) {
                System.out.println("Error deleting main cgroup: " + e.getMessage());
            }
        }
    }

    private ConcurrentMap<Integer, CopyOnWriteArrayList<String>> cgroupCache;
    private boolean cgroupCacheEnabled = true;

    public CgroupCache(boolean cgroupCacheEnabled) {
        createMainCgroup();
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());

        this.cgroupCacheEnabled = cgroupCacheEnabled;
        if (cgroupCacheEnabled) {
            cgroupCache = new ConcurrentHashMap<>();
            warmupCgroupCache();
        }
    }

    public String getCgroupIdByQuota(int quota) {
        if (!cgroupCacheEnabled) {
            return createCgroup(quota);
        }

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
        if (cgroupCacheEnabled) {
            cgroupCache.get(quota).remove(cgroupId);
            System.out.println("Cleared " + cgroupId + " from cache");
        }
        long finish = System.nanoTime();
        System.out.printf("Updated %s (added thread %s) in %s us%n", threadId, cgroupId, (finish - start) / 1000);
    }

    public void removeThreadFromCgroup(String cgroupId, int quota) {
        long start = System.nanoTime();
        int threadId = NativeSandboxInterface.getThreadId();
        NativeSandboxInterface.removeThreadFromCgroup(cgroupId);
        if (cgroupCacheEnabled) {
            cgroupCache.get(quota).add(cgroupId);
            System.out.println("Added " + cgroupId + " to cache");
        } else {
            deleteCgroup(cgroupId);
        }
        long finish = System.nanoTime();
        System.out.printf("Updated %s (deleted thread %s) in %s us%n", threadId, cgroupId, (finish - start) / 1000);
    }

    private void deleteCgroup(String cgroupId) {
        long start = System.nanoTime();
        NativeSandboxInterface.deleteCgroup(cgroupId);
        long finish = System.nanoTime();
        System.out.printf("Deleted %s in %s us%n", cgroupId, (finish - start) / 1000);
    }

    private void warmupCgroupCache() {
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
        if (cgroupCacheEnabled) {
            cgroupCache.get(quota).add(cgroupId);
            System.out.println("Added " + cgroupId + " to cache");
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long finish = System.nanoTime();
        System.out.printf("Created %s in %s us%n", cgroupId, (finish - start) / 1000);

        return cgroupId;
    }

    private void createMainCgroup() {
        long start = System.nanoTime();
        NativeSandboxInterface.createMainCgroup();
        long finish = System.nanoTime();
        System.out.printf("Created main cgroup in %s us%n", (finish - start) / 1000);
    }

    private static void deleteMainCgroup() {
        long start = System.nanoTime();
        NativeSandboxInterface.deleteMainCgroup();
        long finish = System.nanoTime();
        System.out.printf("Deleted main cgroup in %s us%n", (finish - start) / 1000);
    }
}
