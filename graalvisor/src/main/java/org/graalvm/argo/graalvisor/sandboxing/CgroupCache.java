package org.graalvm.argo.graalvisor.sandboxing;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CgroupCache {

    private ConcurrentMap<Integer, CopyOnWriteArrayList<String>> cgroupCache = new ConcurrentHashMap<>();

    public CgroupCache() {
        prepopulateCgroupCache();
    }

    public String getCgroupIdByQuota(int quota) {
        String cgroupId;
        if (!cgroupCache.containsKey(quota)) {
            System.out.println("Creating cached cgroup for " + quota + " CPU quota");
            cgroupCache.put(quota, new CopyOnWriteArrayList<>());
        }

        if (cgroupCache.get(quota).isEmpty()) {
            cgroupId = createCgroup(quota);
        } else {
            cgroupId = cgroupCache.get(quota).get(0);
            System.out.println("Using existing cgroup with " + quota + " CPU quota with ID = " + cgroupId);
        }

        return cgroupId;
    }

    public void insertThreadInCgroup(String cgroupId, int quota) {
        int threadId = NativeSandboxInterface.getThreadId();
        System.out.println("Inserting thread " + threadId + " in cgroup " + cgroupId);
        NativeSandboxInterface.insertThreadInCgroup(cgroupId, String.valueOf(threadId));
        cgroupCache.get(quota).remove(cgroupId);
    }

    public void removeThreadFromCgroup(String cgroupId, int quota) {
        System.out.println("Removing thread " + NativeSandboxInterface.getThreadId() + " from cgroup " + cgroupId);
        NativeSandboxInterface.removeThreadFromCgroup(cgroupId);
        cgroupCache.get(quota).add(cgroupId);
    }

    public void prepopulateCgroupCache() {
        System.out.println("Prepopulating cgroup cache");
        cgroupCache.put(10000, new CopyOnWriteArrayList<>());
        createCgroup(10000);
        cgroupCache.put(100000, new CopyOnWriteArrayList<>());
        createCgroup(100000);
    }

    private String createCgroup(int quota) {
        String cgroupId = "cgroup-" + quota + "-" + UUID.randomUUID();
        System.out.println("Creating new cgroup with " + quota + " CPU quota with ID = " + cgroupId);
        NativeSandboxInterface.createCgroup(cgroupId);
        NativeSandboxInterface.setCgroupQuota(cgroupId, quota);
        cgroupCache.get(quota).add(cgroupId);
        return cgroupId;
    }
}
