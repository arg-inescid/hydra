package com.lambda_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "virtualizeConfig",
        "execBinaries",
        "gateway",
        "timeout",
        "healthCheck",
        "memory",
        "lambdaPort",
        "vmmConsole",
        "managerConsole",
        "managerState"
})
public class ManagerArguments implements Serializable
{
    @JsonProperty("virtualizeConfig")
    private String virtualizeConfig;
    @JsonProperty("execBinaries")
    private String execBinaries;
    @JsonProperty("gateway")
    private String gateway;
    @JsonProperty("timeout")
    private int timeout;
    @JsonProperty("healthCheck")
    private int healthCheck;
    @JsonProperty("memory")
    private String memory;
    @JsonProperty("lambdaPort")
    private int lambdaPort;
    @JsonProperty("vmmConsole")
    private boolean vmmConsole;
    @JsonProperty("managerConsole")
    private boolean managerConsole;
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
     * @param vmmConsole - Is console active during qemu's run.
     * @param managerState - The class that represent state of one manager's instance.
     * @param memory - How maximum memory will consume one active lambda.
     * @param lambdaPort - First port in lambda port space.
     * @param healthCheck - Stated instance health is checked in this time span. After first health response,
     *                    no more check are made.
     * @param timeout - Timeout
     * @param gateway - The Bridge address.
     * @param execBinaries - The Bridge name.
     */
    public ManagerArguments(String virtualizeConfig, String execBinaries, String gateway, int timeout,
                            int healthCheck, String memory, int lambdaPort, boolean vmmConsole, ManagerState managerState) {
        super();
        this.virtualizeConfig = virtualizeConfig;
        this.execBinaries = execBinaries;
        this.gateway = gateway;
        this.timeout = timeout;
        this.healthCheck = healthCheck;
        this.memory = memory;
        this.lambdaPort = lambdaPort;
        this.vmmConsole = vmmConsole;
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

    @JsonProperty("execBinaries")
    public String getExecBinaries() {
        return execBinaries;
    }

    @JsonProperty("execBinaries")
    public void setExecBinaries(String execBinaries) {
        this.execBinaries = execBinaries;
    }

    @JsonProperty("gateway")
    public String getGateway() {
        return gateway;
    }

    @JsonProperty("gateway")
    public void setGateway(String gateway) {
        this.gateway = gateway;
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

    @JsonProperty("vmmConsole")
    public boolean isVmmConsole() {
        return vmmConsole;
    }

    @JsonProperty("vmmConsole")
    public void setVmmConsole(boolean vmmConsole) {
        this.vmmConsole = vmmConsole;
    }

    @JsonProperty("managerConsole")
    public boolean isManagerConsole() {
        return managerConsole;
    }

    @JsonProperty("managerConsole")
    public void setManagerConsole(boolean managerConsole) {
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