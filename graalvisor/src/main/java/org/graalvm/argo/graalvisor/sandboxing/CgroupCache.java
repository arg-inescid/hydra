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

    private ConcurrentMap<Integer, String> threadCgroupMap = new ConcurrentHashMap<>();
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

    public void insertThreadInCgroup(int quota) {
        long start = System.nanoTime();
        String cgroupId;

        if (!cgroupCacheEnabled) {
            cgroupId = createCgroup(quota);
        } else {
            cgroupId = getCgroupIdByQuota(quota);
            if (cgroupId != null) {
                cgroupCache.get(quota).remove(cgroupId);
                System.out.println("Removed " + cgroupId + " from cache");
            } else {
                cgroupId = createCgroup(quota, false);
            }
        }

        int threadId = NativeSandboxInterface.getThreadId();
        NativeSandboxInterface.insertThreadInCgroup(cgroupId, String.valueOf(threadId));
        threadCgroupMap.put(threadId, cgroupId);

        long finish = System.nanoTime();
        System.out.printf("Updated %s (added thread %s) in %s us%n", cgroupId, threadId, (finish - start) / 1000);
    }

    public void removeThreadFromCgroup(int quota) {
        long start = System.nanoTime();
        int threadId = NativeSandboxInterface.getThreadId();
        String cgroupId = threadCgroupMap.remove(threadId);
        NativeSandboxInterface.removeThreadFromCgroup(String.valueOf(threadId));

        if (cgroupCacheEnabled) {
            cgroupCache.get(quota).add(cgroupId);
            long finish = System.nanoTime();
            System.out.printf("Updated %s (deleted thread %s) in %s us%n", cgroupId, threadId, (finish - start) / 1000);
            System.out.println("Added " + cgroupId + " to cache");
        } else {
            deleteCgroup(cgroupId);
        }
    }

    private String getCgroupIdByQuota(int quota) {
        if (cgroupCache.containsKey(quota) && !cgroupCache.get(quota).isEmpty()) {
            return cgroupCache.get(quota).get(0);
        } else {
            return null;
        }
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
        return createCgroup(quota, true);
    }

    private String createCgroup(int quota, boolean toCache) {
        long start = System.nanoTime();
        String cgroupId = "cgroup-" + quota + "-" + UUID.randomUUID();
        NativeSandboxInterface.createCgroup(cgroupId);
        NativeSandboxInterface.setCgroupQuota(cgroupId, quota);
        if (cgroupCacheEnabled) {
            if (!cgroupCache.containsKey(quota)) {
                cgroupCache.put(quota, new CopyOnWriteArrayList<>());
            }
            if (toCache) {
                cgroupCache.get(quota).add(cgroupId);
                System.out.println("Added " + cgroupId + " to cache");
            }
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
