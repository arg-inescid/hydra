package com.serverless_demo.utils;

import com.serverless_demo.code_writer.CodeWriter;
import com.serverless_demo.collectors.lambda_storage.LambdaStorage;
import com.serverless_demo.connectivity.client.LambdaManagerClient;
import com.serverless_demo.core.LambdaManagerConfiguration;
import com.serverless_demo.encoders.Encoder;
import com.serverless_demo.exceptions.argument_parser.ErrorDuringReflectiveClassCreation;
import com.serverless_demo.optimizers.Optimizer;
import com.serverless_demo.schedulers.Scheduler;
import com.serverless_demo.utils.parser.ManagerArguments;
import com.serverless_demo.utils.parser.ManagerState;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class LambdaManagerArgumentStorage {

    private String bridgeName;

    private String nextTap = "t100";
    private final HashMap<String, ArrayList<String>> tapNames = new HashMap<>();

    private int timeout;
    private int healthCheck;
    private String memorySpace;

    private String nextAddress;
    private final HashMap<String, ArrayList<String>> lambdaAddresses = new HashMap<>();

    private int lambdaListeningPort;
    private int nextListeningPort;
    private final HashMap<String, ArrayList<Integer>> portPool = new HashMap<>();

    private boolean isConsoleActive;

    private String virtualizationConfig;

    private String getNextAvailableTapName(ArrayList<String> taps) {
        String resultTap = nextTap;
        nextTap = "t" + (Integer.parseInt(nextTap.split("t")[1]) + 1);
        taps.add(resultTap);
        return resultTap;
    }

    private String getNextAvailableAddress(ArrayList<String> addresses) {
        String[] tmp = nextAddress.split("\\.");
        nextAddress =  tmp[0] + "." + tmp[1] + "." + tmp[2] + "." + (Integer.parseInt(tmp[3]) + 1);
        addresses.add(nextAddress);
        return nextAddress;
    }

    public String getBridgeName() {
        return bridgeName;
    }

    public String getTapName(String lambdaName, int id) {
        ArrayList<String> taps = tapNames.computeIfAbsent(lambdaName, k -> new ArrayList<>());
        return id < taps.size() ? taps.get(id) : getNextAvailableTapName(taps);
    }

    public int getTimeout() {
        return timeout;
    }

    public int getHealthCheck() {
        return healthCheck;
    }

    public String getMemorySpace() {
        return memorySpace;
    }

    public String makeNewInstanceFullAddress(String lambdaName) {
        ArrayList<String> addresses = lambdaAddresses.computeIfAbsent(lambdaName, k -> new ArrayList<>());
        return "http://" + getNextAvailableAddress(addresses) + ":" + lambdaListeningPort;
    }

    public String getInstanceAddress(String lambdaName, int id) {
        return lambdaAddresses.get(lambdaName).get(id);
    }

    public int getInstancePort(String lambdaName, int id) {
        ArrayList<Integer> ports = portPool.computeIfAbsent(lambdaName, k -> new ArrayList<>());
        if (id < ports.size()) {
            return ports.get(id);
        } else {
            ports.add(nextListeningPort);
            return nextListeningPort++;
        }
    }

    public boolean isConsoleActive() {
        return isConsoleActive;
    }

    public int getNumberOfInstances(String lambdaName) {
        return tapNames.get(lambdaName).size();
    }

    private String generateNewTapName() {
        return new Random().ints('a', 'z' + 1)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private Object createObject(String className) throws ErrorDuringReflectiveClassCreation {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InstantiationException | InvocationTargetException e) {
            e.printStackTrace(System.err);
            throw new ErrorDuringReflectiveClassCreation("Error during reflective class creation!", e);
        }
    }

    public LambdaManagerConfiguration initializeLambdaManager(ManagerArguments managerArguments) throws ErrorDuringReflectiveClassCreation {
        this.bridgeName = managerArguments.getBridgeName();
        this.nextAddress = managerArguments.getBridgeAddress();
        this.healthCheck = managerArguments.getHealthCheck();
        this.timeout = 5000;
        this.memorySpace = managerArguments.getMemory();
        this.lambdaListeningPort = managerArguments.getLambdaPort();
        this.nextListeningPort = this.lambdaListeningPort;
        this.isConsoleActive = managerArguments.isConsole();
        this.virtualizationConfig = managerArguments.getVirtualizeConfig();

        ManagerState managerState = managerArguments.getManagerState();
        Scheduler scheduler = (Scheduler) createObject(managerState.getScheduler());
        Optimizer optimizer = (Optimizer) createObject(managerState.getOptimizer());
        Encoder encoder = (Encoder) createObject(managerState.getEncoder());
        LambdaStorage storage = (LambdaStorage) createObject(managerState.getStorage());
        LambdaManagerClient client = (LambdaManagerClient) createObject(managerState.getClient());
        CodeWriter codeWriter = (CodeWriter) createObject(managerState.getCodeWriter());
        return new LambdaManagerConfiguration(scheduler, optimizer, encoder, storage, client, codeWriter, this);
    }

    public String getVirtualizationConfig() {
        return virtualizationConfig;
    }
}
