package com.lambda_manager.utils;

import com.github.maltalex.ineter.base.IPv4Address;
import com.github.maltalex.ineter.range.IPv4Subnet;
import com.lambda_manager.code_writer.FunctionWriter;
import com.lambda_manager.collectors.lambda_storage.LambdaStorage;
import com.lambda_manager.connectivity.client.LambdaManagerClient;
import com.lambda_manager.core.LambdaManagerConfiguration;
import com.lambda_manager.encoders.Encoder;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringReflectiveClassCreation;
import com.lambda_manager.optimizers.Optimizer;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.schedulers.Scheduler;
import com.lambda_manager.utils.logger.CustomFormatter;
import com.lambda_manager.utils.logger.ElapseTimer;
import com.lambda_manager.utils.parser.ManagerArguments;
import com.lambda_manager.utils.parser.ManagerConsole;
import com.lambda_manager.utils.parser.ManagerState;
import io.micronaut.context.BeanContext;
import io.micronaut.http.client.RxHttpClient;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LambdaManagerArgumentStorage {

    public static final int RAND_STRING_LEN = 10;

    private String gateway;
    private String mask;
    private Iterator<IPv4Address> iPv4AddressIterator;

    private int maxLambdas;
    private final ArrayList<ConnectionTriplet<String, String, RxHttpClient>> connectionPool = new ArrayList<>();

    private int timeout;
    private int healthCheck;
    private String memorySpace;
    private int lambdaPort;
    private boolean isLambdaConsoleActive;

    private void initClassFields(ManagerArguments managerArguments) {
        String gatewayString = managerArguments.getGateway();
        this.gateway = gatewayString.split("/")[0];
        IPv4Subnet gatewayWithMask = IPv4Subnet.of(gatewayString);
        this.mask = gatewayWithMask.getNetworkMask().toString();
        this.iPv4AddressIterator = gatewayWithMask.iterator();
        this.iPv4AddressIterator.next();

        this.maxLambdas = managerArguments.getMaxLambdas();

        this.timeout = managerArguments.getTimeout();
        this.healthCheck = managerArguments.getHealthCheck();
        this.memorySpace = managerArguments.getMemory();
        this.lambdaPort = managerArguments.getLambdaPort();
        this.isLambdaConsoleActive = managerArguments.isLambdaConsole();
    }

    private void prepareConnectionPool(LambdaManagerConfiguration configuration, BeanContext beanContext) throws MalformedURLException {
        for (int i = 0; i < maxLambdas; i++) {
            String ip = getNextIPAddress();
            String tap = generateRandomString();
            RxHttpClient client = beanContext.createBean(RxHttpClient.class,
                    new URL("http://" + ip + ":" + lambdaPort));
            connectionPool.add(new ConnectionTriplet<>(ip, tap, client));
        }

        ProcessBuilder createTaps = Processes.CREATE_TAPS.build(null, configuration);
        createTaps.start();
        try {
            createTaps.join();
        } catch (InterruptedException e) {
            // TODO: Proper handling.
            e.printStackTrace();
        }
    }

    private void prepareLogger(ManagerConsole managerConsole) {
        CustomFormatter formatter = new CustomFormatter();
        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

        if (!managerConsole.isTurnOff()) {
            if (managerConsole.isFineGrain()) {
                logger.setLevel(Level.FINE);
            }
            for (Handler handler : logger.getParent().getHandlers()) {
                handler.setFormatter(formatter);
                if (managerConsole.isFineGrain()) {
                    handler.setLevel(Level.FINE);
                }
            }
            if (managerConsole.isRedirectToFile()) {
                File managerLogDir = new File("src/logs/");
                //noinspection ResultOfMethodCallIgnored
                managerLogDir.mkdirs();
                String managerLogFilename = "src/logs/managers/lambda-manager_" + generateRandomString() + ".log";
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
            }
        } else {
            logger.setLevel(Level.OFF);
        }
    }

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

    private LambdaManagerConfiguration prepareConfiguration(ManagerState managerState)
            throws ErrorDuringReflectiveClassCreation {

        Scheduler scheduler = (Scheduler) createObject(managerState.getScheduler());
        Optimizer optimizer = (Optimizer) createObject(managerState.getOptimizer());
        Encoder encoder = (Encoder) createObject(managerState.getEncoder());
        LambdaStorage storage = (LambdaStorage) createObject(managerState.getStorage());
        LambdaManagerClient client = (LambdaManagerClient) createObject(managerState.getClient());
        FunctionWriter functionWriter = (FunctionWriter) createObject(managerState.getCodeWriter());
        return new LambdaManagerConfiguration(scheduler, optimizer, encoder, storage, client, functionWriter, this);
    }

    public LambdaManagerConfiguration initializeLambdaManager(ManagerArguments managerArguments, BeanContext beanContext)
            throws ErrorDuringReflectiveClassCreation, MalformedURLException {

        initClassFields(managerArguments);
        prepareLogger(managerArguments.getManagerConsole());
        LambdaManagerConfiguration configuration = prepareConfiguration(managerArguments.getManagerState());
        prepareConnectionPool(configuration, beanContext);
        ElapseTimer.init(); // Start internal timer.
        return configuration;
    }

    public void cleanupStorage() {
        for (ConnectionTriplet<String, String, RxHttpClient> connectionTriplet : connectionPool) {
            connectionTriplet.client.close();   // Close http client if it's not closed yet.
        }
    }

    public String getNextIPAddress() {
        String nextIPAddress = iPv4AddressIterator.next().toString();
        if (nextIPAddress.equals(getGateway())) {
            return iPv4AddressIterator.next().toString();
        } else {
            return nextIPAddress;
        }
    }

    public String getGateway() {
        return gateway;
    }

    public int getMaxLambdas() {
        return maxLambdas;
    }

    public ArrayList<ConnectionTriplet<String, String, RxHttpClient>> getConnectionPool() {
        return connectionPool;
    }

    public ConnectionTriplet<String, String, RxHttpClient> getNextConnectionTriplet() {
        return connectionPool.remove(0);
    }

    public void returnConnectionTriplet(ConnectionTriplet<String, String, RxHttpClient> connectionTriplet) {
        connectionPool.add(connectionTriplet);
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

    public boolean isLambdaConsoleActive() {
        return isLambdaConsoleActive;
    }

    public String generateRandomString() {
        return new Random().ints('a', 'z' + 1)
                .limit(RAND_STRING_LEN)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
