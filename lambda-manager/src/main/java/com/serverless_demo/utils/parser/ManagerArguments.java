package com.serverless_demo.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "virtualizeConfig",
        "bridgeName",
        "bridgeAddress",
        "healthCheck",
        "memory",
        "lambdaPort",
        "console",
        "managerState"
})
public class ManagerArguments implements Serializable
{
    @JsonProperty("virtualizeConfig")
    private String virtualizeConfig;
    @JsonProperty("bridgeName")
    private String bridgeName;
    @JsonProperty("bridgeAddress")
    private String bridgeAddress;
    @JsonProperty("healthCheck")
    private int healthCheck;
    @JsonProperty("memory")
    private String memory;
    @JsonProperty("lambdaPort")
    private int lambdaPort;
    @JsonProperty("console")
    private boolean console;
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
     * @param console - Is console active during qemu's run.
     * @param managerState - The class that represent state of one manager's instance.
     * @param memory - How maximum memory will consume one active lambda.
     * @param lambdaPort - First port in lambda port space.
     * @param healthCheck - Stated instance health is checked in this time span. After first health response,
     *                    no more check are made.
     * @param bridgeAddress - The Bridge address.
     * @param bridgeName - The Bridge name.
     */
    public ManagerArguments(String virtualizeConfig, String bridgeName, String bridgeAddress, int healthCheck, String memory, int lambdaPort, boolean console, ManagerState managerState) {
        super();
        this.virtualizeConfig = virtualizeConfig;
        this.bridgeName = bridgeName;
        this.bridgeAddress = bridgeAddress;
        this.healthCheck = healthCheck;
        this.memory = memory;
        this.lambdaPort = lambdaPort;
        this.console = console;
        this.managerState = managerState;
    }

    @JsonProperty("virtualizeConfig")
    public String getVirtualizeConfig() {
        return virtualizeConfig;
    }

    @JsonProperty("virtualizeConfig")
    public void setVirtualizeConfig(String virtualizeConfig) {
        this.virtualizeConfig = virtualizeConfig;
    }

    @JsonProperty("bridgeName")
    public String getBridgeName() {
        return bridgeName;
    }

    @JsonProperty("bridgeName")
    public void setBridgeName(String bridgeName) {
        this.bridgeName = bridgeName;
    }

    @JsonProperty("bridgeAddress")
    public String getBridgeAddress() {
        return bridgeAddress;
    }

    @JsonProperty("bridgeAddress")
    public void setBridgeAddress(String bridgeAddress) {
        this.bridgeAddress = bridgeAddress;
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

    @JsonProperty("console")
    public boolean isConsole() {
        return console;
    }

    @JsonProperty("console")
    public void setConsole(boolean console) {
        this.console = console;
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