package com.lambda_manager.collectors.meta_info;

import com.lambda_manager.optimizers.FunctionStatus;
import com.lambda_manager.processes.ProcessBuilder;

import java.util.ArrayList;
import java.util.HashMap;

// TODO: Make this interface.
public class Function {

	// TODO - we don't need this ID! This is super misleading...
    private int id = 0;
    private final String name;
    private FunctionStatus status;
    private boolean updateID;
    private String arguments;
    private final ArrayList<Lambda> availableLambdas = new ArrayList<>();
    private final ArrayList<Lambda> startedLambdas = new ArrayList<>();
    private final ArrayList<Lambda> activeLambdas = new ArrayList<>();
    private final HashMap<Integer, ProcessBuilder> currentlyActiveWorkers = new HashMap<>();

    public Function(String name) {
        this.name = name;
        this.status = FunctionStatus.NOT_BUILT_NOT_CONFIGURED;
    }

    public int getId() {
        return id++;
    }

    public String getName() {
        return name;
    }

    public FunctionStatus getStatus() {
        return status;
    }

    public void setStatus(FunctionStatus status) {
        this.status = status;
    }

    public boolean isUpdateID() {
        return updateID;
    }

    public void setUpdateID(boolean updateID) {
        this.updateID = updateID;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public ArrayList<Lambda> getAvailableLambdas() {
        return availableLambdas;
    }

    public ArrayList<Lambda> getStartedLambdas() {
        return startedLambdas;
    }

    public ArrayList<Lambda> getActiveLambdas() {
        return activeLambdas;
    }

    public HashMap<Integer, ProcessBuilder> getCurrentlyActiveWorkers() {
        return currentlyActiveWorkers;
    }
}
