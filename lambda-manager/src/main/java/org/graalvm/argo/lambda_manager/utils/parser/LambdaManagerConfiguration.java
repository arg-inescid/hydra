package org.graalvm.argo.lambda_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
                "gateway",
                "maxLambdas",
                "timeout",
                "healthCheck",
                "lambdaPort",
                "lambdaConsole",
                "managerConsole",
                "managerState"
})
public class LambdaManagerConfiguration implements Serializable {
    @JsonProperty("gateway") private String gateway;
    @JsonProperty("maxLambdas") private int maxLambdas;
    @JsonProperty("timeout") private int timeout;
    @JsonProperty("healthCheck") private int healthCheck;
    @JsonProperty("lambdaPort") private int lambdaPort;
    @JsonProperty("lambdaConsole") private boolean lambdaConsole;
    @JsonProperty("managerConsole") private LambdaManagerConsole lambdaManagerConsole;
    @JsonProperty("managerState") private LambdaManagerState lambdaManagerState;
    private final static long serialVersionUID = -6081673374812554207L;

    /**
     * No args constructor for use in serialization
     *
     */
    public LambdaManagerConfiguration() {
    }

    /**
     *
     * @param gateway - The default PC's gateway address.
     * @param maxLambdas - How many lambdas can be started in total by this manager.
     * @param timeout - Time during which lambda can stay inactive.
     * @param healthCheck - Lambda's health will be checked in this time-span, after the first
     *            health response, no more checks are made.
     * @param memory - Maximum memory consumption per active lambda.
     * @param lambdaPort - In which port the lambda will receive its requests.
     * @param lambdaConsole - Is console active during qemu's run.
     * @param lambdaManagerConsole - The class with information about manager logging.
     * @param lambdaManagerState - The class that represent state of one manager's instance.
     */
    public LambdaManagerConfiguration(String gateway, int maxLambdas, int timeout, int healthCheck, int lambdaPort,
                    boolean lambdaConsole, LambdaManagerConsole lambdaManagerConsole, LambdaManagerState lambdaManagerState) {
        super();
        this.gateway = gateway;
        this.maxLambdas = maxLambdas;
        this.timeout = timeout;
        this.healthCheck = healthCheck;
        this.lambdaPort = lambdaPort;
        this.lambdaConsole = lambdaConsole;
        this.lambdaManagerConsole = lambdaManagerConsole;
        this.lambdaManagerState = lambdaManagerState;
    }

    @JsonProperty("gateway")
    public String getGateway() {
        return gateway;
    }

    @JsonProperty("gateway")
    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    @JsonProperty("maxLambdas")
    public int getMaxLambdas() {
        return maxLambdas;
    }

    @JsonProperty("maxLambdas")
    public void setMaxLambdas(int maxLambdas) {
        this.maxLambdas = maxLambdas;
    }

    @JsonProperty("timeout")
    public int getTimeout() {
        return timeout;
    }

    @JsonProperty("timeout")
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @JsonProperty("healthCheck")
    public int getHealthCheck() {
        return healthCheck;
    }

    @JsonProperty("healthCheck")
    public void setHealthCheck(int healthCheck) {
        this.healthCheck = healthCheck;
    }

    @JsonProperty("lambdaPort")
    public int getLambdaPort() {
        return lambdaPort;
    }

    @JsonProperty("lambdaPort")
    public void setLambdaPort(int lambdaPort) {
        this.lambdaPort = lambdaPort;
    }

    @JsonProperty("lambdaConsole")
    public boolean isLambdaConsole() {
        return lambdaConsole;
    }

    @JsonProperty("lambdaConsole")
    public void setLambdaConsole(boolean lambdaConsole) {
        this.lambdaConsole = lambdaConsole;
    }

    @JsonProperty("managerConsole")
    public LambdaManagerConsole getManagerConsole() {
        return lambdaManagerConsole;
    }

    @JsonProperty("managerConsole")
    public void setManagerConsole(LambdaManagerConsole lambdaManagerConsole) {
        this.lambdaManagerConsole = lambdaManagerConsole;
    }

    @JsonProperty("managerState")
    public LambdaManagerState getManagerState() {
        return lambdaManagerState;
    }

    @JsonProperty("managerState")
    public void setManagerState(LambdaManagerState lambdaManagerState) {
        this.lambdaManagerState = lambdaManagerState;
    }

}