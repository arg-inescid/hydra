package org.graalvm.argo.lambda_manager.core;

import com.github.maltalex.ineter.range.IPv4Subnet;
import org.graalvm.argo.lambda_manager.client.DefaultLambdaManagerClient;
import org.graalvm.argo.lambda_manager.encoders.DefaultCoder;
import org.graalvm.argo.lambda_manager.function_storage.LocalFunctionStorage;
import org.graalvm.argo.lambda_manager.metrics.MetricsScraper;
import org.graalvm.argo.lambda_manager.processes.ProcessBuilder;
import org.graalvm.argo.lambda_manager.processes.devmapper.PrepareDevmapperBase;
import org.graalvm.argo.lambda_manager.processes.lambda.factory.AbstractLambdaFactory;
import org.graalvm.argo.lambda_manager.processes.lambda.factory.ContainerLambdaFactory;
import org.graalvm.argo.lambda_manager.processes.lambda.factory.FirecrackerCtrLambdaFactory;
import org.graalvm.argo.lambda_manager.processes.lambda.factory.FirecrackerLambdaFactory;
import org.graalvm.argo.lambda_manager.processes.lambda.factory.FirecrackerSnapshotLambdaFactory;
import org.graalvm.argo.lambda_manager.schedulers.RoundedRobinScheduler;
import org.graalvm.argo.lambda_manager.utils.Messages;
import org.graalvm.argo.lambda_manager.utils.logger.ElapseTimer;
import org.graalvm.argo.lambda_manager.utils.logger.LambdaManagerFormatter;
import org.graalvm.argo.lambda_manager.utils.logger.Logger;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerConfiguration;
import org.graalvm.argo.lambda_manager.utils.parser.LambdaManagerConsole;
import io.micronaut.context.BeanContext;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

public class ArgumentStorage {

    /**
     * Type of workers: vm_firecracker, vm_containerd, or container.
     */
    private LambdaType lambdaType;

    /**
     * Factory to instantiate heterogeneous lambda processes.
     */
    private AbstractLambdaFactory lambdaFactory;

    /**
     * Pool of ready to use lambdas.
     */
    private LambdaPool lambdaPool;

    // TODO - add comments to the rest of these fields.
    private String gateway;
    private String mask;
    private int timeout;
    private int healthCheck;
    private int lambdaPort;
    private int maxMemory;
    private int cpuQuota;
    private boolean isLambdaConsoleActive; // TODO - do we ever disable this?

    /**
     * Scheduled worker that writes metrics to a file.
     */
    private MetricsScraper metricsScraper;

    /* Private constructor. */
    private ArgumentStorage() { }

    private void initClassFields(LambdaManagerConfiguration lambdaManagerConfiguration) {
        this.gateway = lambdaManagerConfiguration.getGateway().split("/")[0];
        this.mask = IPv4Subnet.of(lambdaManagerConfiguration.getGateway()).getNetworkMask().toString();
        this.lambdaType = LambdaType.fromString(lambdaManagerConfiguration.getLambdaType());
        this.maxMemory = lambdaManagerConfiguration.getMaxMemory();
        this.cpuQuota = ((maxMemory * 1000 / 1024) / 2) * 100; // MiB to MB; divide by two due to ratio; multiply by 100 to get cgroups quota.
        this.lambdaPool = new LambdaPool(lambdaType, lambdaManagerConfiguration.getMaxTaps());
        this.timeout = lambdaManagerConfiguration.getTimeout();
        this.healthCheck = lambdaManagerConfiguration.getHealthCheck();
        this.lambdaPort = lambdaManagerConfiguration.getLambdaPort();
        initLambdaFactory(this.lambdaType);
        this.isLambdaConsoleActive = lambdaManagerConfiguration.isLambdaConsole();
    }

    private void initLambdaFactory(LambdaType lambdaType) {
        switch (lambdaType) {
            case VM_FIRECRACKER:
                this.lambdaFactory = new FirecrackerLambdaFactory();
                break;
            case VM_FIRECRACKER_SNAPSHOT:
                this.lambdaFactory = new FirecrackerSnapshotLambdaFactory();
                break;
            case VM_CONTAINERD:
                this.lambdaFactory = new FirecrackerCtrLambdaFactory();
                break;
            case CONTAINER:
            case CONTAINER_DEBUG:
                this.lambdaFactory = new ContainerLambdaFactory();
                break;
            default:
                throw new IllegalStateException("Could not instantiate lambda factory due to unknown lambda type: " + lambdaType);
        }
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

    private void initMetricsScraper() {
        try {
            File managerMetricsFile = new File(Environment.MANAGER_METRICS_FILENAME);
            managerMetricsFile.getParentFile().mkdirs();
            managerMetricsFile.createNewFile();

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            metricsScraper = new MetricsScraper(managerMetricsFile, executor);
            executor.scheduleAtFixedRate(metricsScraper, 1, 5, TimeUnit.SECONDS);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public void doInitialize(LambdaManagerConfiguration lambdaManagerConfiguration, BeanContext beanContext) throws InterruptedException {

        initClassFields(lambdaManagerConfiguration);

        initErrorHandler();

        prepareLogger(lambdaManagerConfiguration.getManagerConsole());
        initMetricsScraper();

        Configuration.initFields(
            new RoundedRobinScheduler(),
            new DefaultCoder(),
            new LocalFunctionStorage(),
            new DefaultLambdaManagerClient(),
            this);

        if (lambdaType == LambdaType.VM_FIRECRACKER || lambdaType == LambdaType.VM_FIRECRACKER_SNAPSHOT) {
            prepareDevmapper();
        }

        ElapseTimer.init(); // Start internal timer.

        this.lambdaPool.setUp(beanContext, lambdaPort, lambdaManagerConfiguration.getGateway(), lambdaManagerConfiguration.getLambdaPool());
    }

    private void prepareDevmapper() {
        try {
            ProcessBuilder prepareDevmapperBase = new PrepareDevmapperBase().build();
            prepareDevmapperBase.start();
            prepareDevmapperBase.join();
        } catch (InterruptedException e) {
            throw new IllegalStateException("Could not prepare devmapper base: " + e);
        }
    }

    public static void initializeLambdaManager(LambdaManagerConfiguration lambdaManagerConfiguration, BeanContext beanContext) throws InterruptedException {
        new ArgumentStorage().doInitialize(lambdaManagerConfiguration, beanContext);
    }

    public String getGateway() {
        return gateway;
    }

    public LambdaPool getLambdaPool() {
        return lambdaPool;
    }

    public String getMask() {
        return mask;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getMaxMemory() {
        return maxMemory;
    }

    public int getCpuQuota() {
        return cpuQuota;
    }

    public int getHealthCheck() {
        return healthCheck;
    }

    public int getLambdaPort() {
        return lambdaPort;
    }

    public LambdaType getLambdaType() {
        return lambdaType;
    }

    public boolean isSnapshotEnabled() {
        return lambdaType == LambdaType.VM_FIRECRACKER_SNAPSHOT;
    }

    public boolean isDebugMode() {
        return lambdaType == LambdaType.CONTAINER_DEBUG;
    }

    public AbstractLambdaFactory getLambdaFactory() {
        return lambdaFactory;
    }

    public boolean isLambdaConsoleActive() {
        return isLambdaConsoleActive;
    }

    public void tearDownMetricsScraper() {
        metricsScraper.close();
    }
}
