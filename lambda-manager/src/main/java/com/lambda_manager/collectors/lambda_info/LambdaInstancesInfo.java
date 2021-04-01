package com.lambda_manager.collectors.lambda_info;

import com.lambda_manager.optimizers.LambdaStatusType;
import com.lambda_manager.processes.ProcessBuilder;
import io.micronaut.http.client.RxHttpClient;

import java.util.ArrayList;
import java.util.HashMap;

public class LambdaInstancesInfo {

    private int id = 0;
    private final String name;
    private LambdaStatusType status;
    private final ArrayList<LambdaInstanceInfo> availableInstances = new ArrayList<>();
    private final ArrayList<LambdaInstanceInfo> startedInstances = new ArrayList<>();
    private final ArrayList<LambdaInstanceInfo> activeInstances = new ArrayList<>();
    private final HashMap<Integer, ProcessBuilder> currentlyActiveWorkers = new HashMap<>();

    public LambdaInstancesInfo(String name) {
        this.name = name;
        this.status = LambdaStatusType.NOT_BUILT_NOT_CONFIGURED;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public LambdaStatusType getStatus() {
        return status;
    }

    public void setStatus(LambdaStatusType status) {
        this.status = status;
    }

    public ArrayList<LambdaInstanceInfo> getAvailableInstances() {
        return availableInstances;
    }

    public ArrayList<LambdaInstanceInfo> getStartedInstances() {
        return startedInstances;
    }

    public ArrayList<LambdaInstanceInfo> getActiveInstances() {
        return activeInstances;
    }

    public HashMap<Integer, ProcessBuilder> getCurrentlyActiveWorkers() {
        return currentlyActiveWorkers;
    }
}
