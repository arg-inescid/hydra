package org.graalvm.argo.lambda_manager.core;

import com.github.maltalex.ineter.base.IPv4Address;
import com.github.maltalex.ineter.range.IPv4Subnet;
import org.graalvm.argo.lambda_manager.client.LambdaManagerClient;
import org.graalvm.argo.lambda_manager.encoders.Coder;
import org.graalvm.argo.lambda_manager.exceptions.argument_parser.ErrorDuringReflectiveClassCreation;
import org.graalvm.argo.lambda_manager.exceptions.user.ErrorDuringCreatingConnectionPool;
import org.graalvm.argo.lambda_manager.function_storage.FunctionStorage;
import org.graalvm.argo.lambda_manager.optimizers.FunctionOptimizer;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.taps.CreateTaps;
import org.graalvm.argo.lambda_manager.schedulers.Scheduler;
import org.graalvm.argo.lambda_manager.utils.ConnectionTriplet;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.ElapseTimer;
import org.graalvm.argo.lambda_manager.utils.logger.LambdaManagerFormatter;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerConfiguration;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerConsole;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerState;
import io.micronaut.context.BeanContext;
import io.micronaut.http.client.RxHttpClient;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

public class ArgumentStorage {

    // TODO - could we just keep the LambdaManagerConfiguration and avoid most of these fields?
    private String gateway;
    private String mask;
    private Iterator<IPv4Address> iPv4AddressIterator;
    private int maxMemory;
    private int maxTaps;
    private final ArrayList<ConnectionTriplet<String, String, RxHttpClient>> connectionPool;
    private int timeout;
    private int healthCheck;
    private int lambdaPort;
    private boolean isLambdaConsoleActive;
    private LambdaManagerConsole cachedConsoleInfo;

    private ArgumentStorage() {
        this.connectionPool = new ArrayList<>();
    }

    private void initClassFields(LambdaManagerConfiguration lambdaManagerConfiguration) {
        this.gateway = lambdaManagerConfiguration.getGateway().split("/")[0];
        IPv4Subnet gatewayWithMask = IPv4Subnet.of(lambdaManagerConfiguration.getGateway());
        this.mask = gatewayWithMask.getNetworkMask().toString();
        this.iPv4AddressIterator = gatewayWithMask.iterator();
        this.iPv4AddressIterator.next();
        this.maxMemory = lambdaManagerConfiguration.getMaxMemory();
        this.maxTaps = lambdaManagerConfiguration.getMaxTaps();
        this.timeout = lambdaManagerConfiguration.getTimeout();
        this.healthCheck = lambdaManagerConfiguration.getHealthCheck();
        this.lambdaPort = lambdaManagerConfiguration.getLambdaPort();
        this.isLambdaConsoleActive = lambdaManagerConfiguration.isLambdaConsole();
    }

    private void initErrorHandler() {
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if (e instanceof IOException) {
                // Irrelevant network problem or API that throws on cancellation.
                Logger.log(Level.WARNING, Messages.INTERNAL_ERROR, e);
                return;
            }
            if (e instanceof InterruptedException) {
                // Fine, some blocking code was interrupted by a disposed call.
                Logger.log(Level.WARNING, Messages.INTERNAL_ERROR, e);
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // That's likely a bug in the application.
                Logger.log(Level.WARNING, Messages.INTERNAL_ERROR, e);
                return;
            }
            if (e instanceof IllegalStateException) {
                // That's a bug in RxJava or in a custom operator.
                Logger.log(Level.WARNING, Messages.INTERNAL_ERROR, e);
                return;
            }
            // TODO: We should discuss what to do in case of severe exceptions.
            Logger.log(Level.SEVERE, Messages.UNDELIVERABLE_EXCEPTION, e);
        });
    }

    private void prepareConnectionPool(BeanContext beanContext) throws ErrorDuringCreatingConnectionPool {
        try {
            for (int i = 0; i < maxTaps; i++) {
                String ip = getNextIPAddress();
                String tap = String.format("%s-%s", Environment.TAP_PREFIX, generateRandomString());
                RxHttpClient client = beanContext.createBean(RxHttpClient.class,
                                new URL("http", ip, lambdaPort, "/"));
                connectionPool.add(new ConnectionTriplet<>(ip, tap, client));
            }

            ProcessBuilder createTaps = new CreateTaps().build();
            createTaps.start();
            createTaps.join();
        } catch (InterruptedException | MalformedURLException e) {
            throw new ErrorDuringCreatingConnectionPool(Messages.ERROR_POOL_CREATION, e);
        }
    }

    private void prepareLogger(LambdaManagerConsole lambdaManagerConsole) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        LambdaManagerFormatter formatter = new LambdaManagerFormatter();

        if (lambdaManagerConsole.isTurnOn()) {
            for (Handler loggerHandler : logger.getParent().getHandlers()) {
                loggerHandler.setFormatter(formatter);
                if (lambdaManagerConsole.isFineGrain()) {
                    loggerHandler.setLevel(Level.FINE);
                }
            }

            if (lambdaManagerConsole.isRedirectToFile()) {
                logger.log(Level.INFO, String.format(Messages.LOG_REDIRECTION,
                                Paths.get(System.getProperty("user.dir"), Environment.MANAGER_LOG_FILENAME)));
                logger.setUseParentHandlers(false);
                try {
                    File managerLogFile = new File(Environment.MANAGER_LOG_FILENAME);
                    managerLogFile.getParentFile().mkdirs();
                    managerLogFile.createNewFile();
                    FileHandler fileHandler = new FileHandler(Environment.MANAGER_LOG_FILENAME, true);
                    fileHandler.setFormatter(formatter);
                    logger.addHandler(fileHandler);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            if (lambdaManagerConsole.isFineGrain()) {
                logger.setLevel(Level.FINE);
            }
        } else {
            logger.setLevel(Level.OFF);
        }

        Logger.setLogger(logger);
    }

    private void cacheConsoleInfo(LambdaManagerConsole lambdaManagerConsole) {
        this.cachedConsoleInfo = lambdaManagerConsole;
    }

    private void prepareLogging(LambdaManagerConsole lambdaManagerConsole) {
        prepareLogger(lambdaManagerConsole);
        cacheConsoleInfo(lambdaManagerConsole);
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

    private void prepareConfiguration(LambdaManagerState lambdaManagerState)
                    throws ErrorDuringReflectiveClassCreation {
        Scheduler scheduler = (Scheduler) createObject(lambdaManagerState.getScheduler());
        FunctionOptimizer optimizer = (FunctionOptimizer) createObject(lambdaManagerState.getOptimizer());
        Coder encoder = (Coder) createObject(lambdaManagerState.getEncoder());
        FunctionStorage storage = (FunctionStorage) createObject(lambdaManagerState.getStorage());
        LambdaManagerClient client = (LambdaManagerClient) createObject(lambdaManagerState.getClient());
        Configuration.initFields(scheduler, optimizer, encoder, storage, client, this);
    }

    public void doInitialize(LambdaManagerConfiguration lambdaManagerConfiguration, BeanContext beanContext)
                    throws ErrorDuringReflectiveClassCreation, ErrorDuringCreatingConnectionPool {
        initClassFields(lambdaManagerConfiguration);
        initErrorHandler();
        prepareLogging(lambdaManagerConfiguration.getManagerConsole());
        prepareConfiguration(lambdaManagerConfiguration.getManagerState());
        prepareConnectionPool(beanContext);
        ElapseTimer.init(); // Start internal timer.
    }

    public static void initializeLambdaManager(LambdaManagerConfiguration lambdaManagerConfiguration, BeanContext beanContext)
                    throws ErrorDuringReflectiveClassCreation, ErrorDuringCreatingConnectionPool {
        ArgumentStorage argumentStorage = new ArgumentStorage();
        argumentStorage.doInitialize(lambdaManagerConfiguration, beanContext);
    }

    public void prepareHandler() {
        LambdaManagerFormatter formatter = new LambdaManagerFormatter();
        Handler handler = new ConsoleHandler();
        handler.setFormatter(formatter);

        if (cachedConsoleInfo.isTurnOn()) {
            if (cachedConsoleInfo.isRedirectToFile()) {
                try {
                    handler = new FileHandler(Environment.MANAGER_LOG_FILENAME, true);
                    handler.setFormatter(formatter);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }

            if (cachedConsoleInfo.isFineGrain()) {
                handler.setLevel(Level.FINE);
            }
        } else {
            handler.setLevel(Level.OFF);
        }

        Logger.setHandler(handler);
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

    public int getMaxMemory() {
        return maxMemory;
    }

    public ArrayList<ConnectionTriplet<String, String, RxHttpClient>> getConnectionPool() {
        return connectionPool;
    }

    public ConnectionTriplet<String, String, RxHttpClient> nextConnectionTriplet() {
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

    public int getLambdaPort() {
        return lambdaPort;
    }

    public boolean isLambdaConsoleActive() {
        return isLambdaConsoleActive;
    }

    public String generateRandomString() {
        return new Random().ints('a', 'z' + 1)
                        .limit(Environment.RAND_STRING_LEN)
                        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                        .toString();
    }
}
