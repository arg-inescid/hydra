package com.lambda_manager.utils;

import com.github.maltalex.ineter.base.IPv4Address;
import com.github.maltalex.ineter.range.IPv4Subnet;
import com.lambda_manager.code_writer.CodeWriter;
import com.lambda_manager.collectors.lambda_storage.LambdaStorage;
import com.lambda_manager.connectivity.client.LambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.encoders.Encoder;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringReflectiveClassCreation;
import com.lambda_manager.optimizers.Optimizer;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.schedulers.Scheduler;
import com.lambda_manager.utils.parser.ManagerArguments;
import com.lambda_manager.utils.parser.ManagerState;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class LambdaManagerArgumentStorage {

    private String execBinaries;

    private String nextTap = "t100";
    private final HashMap<String, ArrayList<String>> tapNames = new HashMap<>();

    private int timeout;
    private int healthCheck;
    private String memorySpace;

    private final HashMap<String, ArrayList<String>> lambdaAddresses = new HashMap<>();
    private String gateway;
    private String mask;
    private Iterator<IPv4Address> iPv4AddressIterator;

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

    public int getTimeout() {
        return timeout;
    }

    public int getHealthCheck() {
        return healthCheck;
    }

    public String getMemorySpace() {
        return memorySpace;
    }

    public String getInstanceAddress(String lambdaName, int id) {
        return lambdaAddresses.get(lambdaName).get(id);
    }

    public int getLambdaListeningPort() {
        return lambdaListeningPort;
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

    public List<Tuple<String, String>> getTapIfPool() {
        return tapIpPool;
    }

    List<Tuple<String, String>> tapIpPool = new ArrayList<>();
    List<Integer> listeningPortPool = new ArrayList<>();

    public Tuple<String, String> getTapIp() {
        return tapIpPool.remove(0);
    }

    public void returnTapIp(Tuple<String, String> tapIp) {
        tapIpPool.add(tapIp);
    }

    public int getNextPort() {
        return listeningPortPool.remove(0);
    }

    public void returnPort(int port) {
        listeningPortPool.add(port);
    }

    private int generateNextPort() {
        return nextListeningPort++;
    }

    private String generateNewTapName() {
        return new Random().ints('a', 'z' + 1)
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private void generateConnections(LambdaManagerConfiguration configuration) {
        ProcessBuilder[] processBuilders = new ProcessBuilder[10];
        for(int i = 0; i < 10; i++) {
            tapIpPool.add(new Tuple<>(generateNewTapName(), iPv4AddressIterator.next().toString()));
            listeningPortPool.add(generateNextPort());
        }

        for (int i = 0; i < 10; i++) {
            processBuilders[i] = Processes.CREATE_TAP.build(null, configuration);
            processBuilders[i].start();
        }

        for (ProcessBuilder processBuilder: processBuilders) {
            try {
                processBuilder.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
        this.virtualizationConfig = managerArguments.getVirtualizeConfig();
        this.execBinaries = managerArguments.getExecBinaries();
        this.timeout = managerArguments.getTimeout();
        this.healthCheck = managerArguments.getHealthCheck();
        this.memorySpace = managerArguments.getMemory();
        this.lambdaListeningPort = managerArguments.getLambdaPort();
        this.nextListeningPort = this.lambdaListeningPort;
        this.isConsoleActive = managerArguments.isConsole();

        String gatewayString = managerArguments.getGateway();
        this.gateway = gatewayString.split("/")[0];
        IPv4Subnet gatewayWithMask = IPv4Subnet.of(gatewayString);
        this.mask = gatewayWithMask.getNetworkMask().toString();
        this.iPv4AddressIterator = gatewayWithMask.iterator();
        this.iPv4AddressIterator.next();

        ManagerState managerState = managerArguments.getManagerState();
        Scheduler scheduler = (Scheduler) createObject(managerState.getScheduler());
        Optimizer optimizer = (Optimizer) createObject(managerState.getOptimizer());
        Encoder encoder = (Encoder) createObject(managerState.getEncoder());
        LambdaStorage storage = (LambdaStorage) createObject(managerState.getStorage());
        LambdaManagerClient client = (LambdaManagerClient) createObject(managerState.getClient());
        CodeWriter codeWriter = (CodeWriter) createObject(managerState.getCodeWriter());
        LambdaManagerConfiguration configuration = new LambdaManagerConfiguration(scheduler,
                optimizer, encoder, storage, client, codeWriter, this);

        generateConnections(configuration);
        return configuration;
    }

    public String getVirtualizationConfig() {
        return virtualizationConfig;
    }

    public String getGateway() {
        return gateway;
    }

    public String getMask() {
        return mask;
    }

    public String getExecBinaries() {
        return execBinaries;
    }
}
