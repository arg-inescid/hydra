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
import com.lambda_manager.schedulers.Scheduler;
import com.lambda_manager.utils.logger.AdvancedOutputFormatter;
import com.lambda_manager.utils.logger.ElapseTimer;
import com.lambda_manager.utils.parser.ManagerArguments;
import com.lambda_manager.utils.parser.ManagerState;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LambdaManagerArgumentStorage {

    public static final int RAND_STRING_LEN = 10;

    private String virtualizationConfig;
    private String execBinaries;

    private String gateway;
    private String mask;
    private Iterator<IPv4Address> iPv4AddressIterator;

    private int timeout;
    private int healthCheck;
    private String memorySpace;

    private int lambdaPort;
    private int nextPort;

    private boolean isVmmConsoleActive;
  
    private Object createObject(String className) throws ErrorDuringReflectiveClassCreation {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                | InstantiationException | InvocationTargetException e) {
            throw new ErrorDuringReflectiveClassCreation("Error during reflective class creation!", e);
        }
    }

    public LambdaManagerConfiguration initializeLambdaManager(ManagerArguments managerArguments)
            throws ErrorDuringReflectiveClassCreation {

        this.virtualizationConfig = managerArguments.getVirtualizeConfig();
        this.execBinaries = managerArguments.getExecBinaries();

        String gatewayString = managerArguments.getGateway();
        this.gateway = gatewayString.split("/")[0];
        IPv4Subnet gatewayWithMask = IPv4Subnet.of(gatewayString);
        this.mask = gatewayWithMask.getNetworkMask().toString();
        this.iPv4AddressIterator = gatewayWithMask.iterator();
        this.iPv4AddressIterator.next();

        this.timeout = managerArguments.getTimeout();
        this.healthCheck = managerArguments.getHealthCheck();
        this.memorySpace = managerArguments.getMemory();

        this.lambdaPort = managerArguments.getLambdaPort();
        this.nextPort = this.lambdaPort;

        this.isVmmConsoleActive = managerArguments.isVmmConsole();

        AdvancedOutputFormatter formatter = new AdvancedOutputFormatter();
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        if (!managerArguments.isManagerConsole()) {
            File managerLogDir = new File("src/logs/");
            //noinspection ResultOfMethodCallIgnored
            managerLogDir.mkdirs();
            String managerLogFilename = "src/logs/lambda-manager_" + generateRandomString() + ".log";
            logger.log(Level.INFO, "Log is redirected to file -> " + managerLogFilename);
            logger.setUseParentHandlers(false);
            try {
                File managerLogFile = new File(managerLogFilename);
                //noinspection ResultOfMethodCallIgnored
                managerLogFile.createNewFile();
                Handler handler = new FileHandler(managerLogFilename);
                logger.addHandler(handler);
                handler.setFormatter(formatter);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } else {
            for (Handler handler : logger.getParent().getHandlers()) {
                handler.setFormatter(formatter);
            }
        }

        ElapseTimer.init();

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

    public String getExecBinaries() {
        return execBinaries;
    }

    public String getNextIPAddress() {
        String nextIPAddress = iPv4AddressIterator.next().toString();
        if(nextIPAddress.equals(getGateway())) {
            return iPv4AddressIterator.next().toString();
        } else {
            return nextIPAddress;
        }
    }

    public String getGateway() {
        return gateway;
    }

    public String getMask() {
        return mask;
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

    public int getLambdaPort() {
        return lambdaPort;
    }

    public int getNextPort() {
        return nextPort++;
    }

    public boolean isVmmConsoleActive() {
        return isVmmConsoleActive;
    }

    public String generateRandomString() {
        return new Random().ints('a', 'z' + 1)
                .limit(RAND_STRING_LEN)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
