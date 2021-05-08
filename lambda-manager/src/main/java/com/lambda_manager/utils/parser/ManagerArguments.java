package com.lambda_manager.utils.parser;

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
        "memory",
        "lambdaPort",
        "lambdaConsole",
        "managerConsole",
        "managerState"
})
public class ManagerArguments implements Serializable
{
    @JsonProperty("gateway")
    private String gateway;
    @JsonProperty("maxLambdas")
    private int maxLambdas;
    @JsonProperty("timeout")
    private int timeout;
    @JsonProperty("healthCheck")
    private int healthCheck;
    @JsonProperty("memory")
    private String memory;
    @JsonProperty("lambdaPort")
    private int lambdaPort;
    @JsonProperty("lambdaConsole")
    private boolean lambdaConsole;
    @JsonProperty("managerConsole")
    private ManagerConsole managerConsole;
    @JsonProperty("managerState")
    private ManagerState managerState;
    private final static long serialVersionUID = -6081673374812554207L;

    /**
     * No args constructor for use in serialization
     *
     */
    public ManagerArguments() {
    }

    /**
     *
     * @param gateway - The default PC's gateway address.
     * @param maxLambdas - How many lambdas can be started in total by this manager.
     * @param timeout - Time during which lambda can stay inactive.
     * @param healthCheck - Lambda's health will be checked in this time-span, after the first health response,
     *                      no more checks are made.
     * @param memory - Maximum memory consumption per active lambda.
     * @param lambdaPort - In which port the lambda will receive it's requests.
     * @param lambdaConsole - Is console active during qemu's run.
     * @param managerConsole - The class with information about manager logging.
     * @param managerState - The class that represent state of one manager's instance.
     */
    public ManagerArguments(String gateway, int maxLambdas, int timeout, int healthCheck, String memory, int lambdaPort,
                            boolean lambdaConsole, ManagerConsole managerConsole, ManagerState managerState) {
        super();
        this.gateway = gateway;
        this.maxLambdas = maxLambdas;
        this.timeout = timeout;
        this.healthCheck = healthCheck;
        this.memory = memory;
        this.lambdaPort = lambdaPort;
        this.lambdaConsole = lambdaConsole;
        this.managerConsole = managerConsole;
        this.managerState = managerState;
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

    @JsonProperty("memory")
    public String getMemory() {
        return memory;
    }

    @JsonProperty("memory")
    public void setMemory(String memory) {
        this.memory = memory;
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
    public ManagerConsole getManagerConsole() {
        return managerConsole;
    }

    @JsonProperty("managerConsole")
    public void setManagerConsole(ManagerConsole managerConsole) {
        this.managerConsole = managerConsole;
    }

    @JsonProperty("managerState")
    public ManagerState getManagerState() {
        return managerState;
    }

    @JsonProperty("managerState")
    public void setManagerState(ManagerState managerState) {
        this.managerState = managerState;
    }

}