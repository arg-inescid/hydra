package org.graalvm.argo.lambda_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "firstLambdaPort",
        "faultTolerance",
        "lambdaFaultTolerance",
        "reclamationInterval",
        "lruReclamationPeriod",
        "reclamationThreshold",
        "reclamationPercentage"
})
public class VariablesConfiguration implements Serializable {
    @JsonProperty("firstLambdaPort") private int firstLambdaPort;
    @JsonProperty("faultTolerance") private int faultTolerance;
    @JsonProperty("lambdaFaultTolerance") private int lambdaFaultTolerance;
    @JsonProperty("reclamationInterval") private int reclamationInterval;
    @JsonProperty("lruReclamationPeriod") private int lruReclamationPeriod;
    @JsonProperty("reclamationThreshold") private float reclamationThreshold;
    @JsonProperty("reclamationPercentage") private float reclamationPercentage;

    private final static long serialVersionUID = -4116566831411247735L;

    /**
     * No args constructor for use in serialization
     */
    public VariablesConfiguration() {
    }

    /**
     * @param firstLambdaPort - For container lambdas only; the first port in the port range for lambdas.
     * @param faultTolerance - Number of times a request will be re-sent to a particular Lambda upon an error.
     * @param lambdaFaultTolerance - Number of times a request will be sent to a different Lambda upon timeout.
     * @param reclamationInterval - Interval in ms for the lambda pool reclamation daemon (i.e., the daemon will potentially reclaim lambdas every X ms).
     * @param lruReclamationPeriod - Only reclaim lambdas that were used more than X ms ago.
     * @param reclamationThreshold - If the lambda pool has less than X% available lambdas, then the reclamation daemon should start reclaiming lambdas. Accepted values - [0; 1].
     * @param reclamationPercentage - Percentage of lambdas to reclaim. Should always be smaller than reclamationThreshold (can be set to reclamationThreshold / 2). Accepted values - [0; reclamationThreshold].
     */
    public VariablesConfiguration(int firstLambdaPort, int faultTolerance, int lambdaFaultTolerance, int reclamationInterval, int lruReclamationPeriod, float reclamationThreshold, float reclamationPercentage) {
        super();
        this.firstLambdaPort = firstLambdaPort;
        this.faultTolerance = faultTolerance;
        this.lambdaFaultTolerance = lambdaFaultTolerance;
        this.reclamationInterval = reclamationInterval;
        this.lruReclamationPeriod = lruReclamationPeriod;
        this.reclamationThreshold = reclamationThreshold;
        this.reclamationPercentage = reclamationPercentage;
    }

    @JsonProperty("firstLambdaPort")
    public int getFirstLambdaPort() {
        return firstLambdaPort;
    }

    @JsonProperty("firstLambdaPort")
    public void setFirstLambdaPort(int firstLambdaPort) {
        this.firstLambdaPort = firstLambdaPort;
    }

    @JsonProperty("faultTolerance")
    public int getFaultTolerance() {
        return faultTolerance;
    }

    @JsonProperty("faultTolerance")
    public void setFaultTolerance(int lambdaFaultTolerance) {
        this.faultTolerance = lambdaFaultTolerance;
    }

    @JsonProperty("lambdaFaultTolerance")
    public int getLambdaFaultTolerance() {
        return lambdaFaultTolerance;
    }

    @JsonProperty("lambdaFaultTolerance")
    public void setLambdaFaultTolerance(int lambdaFaultTolerance) {
        this.lambdaFaultTolerance = lambdaFaultTolerance;
    }

    @JsonProperty("reclamationInterval")
    public int getReclamationInterval() {
        return reclamationInterval;
    }

    @JsonProperty("reclamationInterval")
    public void setReclamationInterval(int reclamationInterval) {
        this.reclamationInterval = reclamationInterval;
    }

    @JsonProperty("lruReclamationPeriod")
    public int getLruReclamationPeriod() {
        return lruReclamationPeriod;
    }

    @JsonProperty("lruReclamationPeriod")
    public void setLruReclamationPeriod(int lruReclamationPeriod) {
        this.lruReclamationPeriod = lruReclamationPeriod;
    }

    @JsonProperty("reclamationThreshold")
    public float getReclamationThreshold() {
        return reclamationThreshold;
    }

    @JsonProperty("reclamationThreshold")
    public void setReclamationThreshold(float reclamationThreshold) {
        this.reclamationThreshold = reclamationThreshold;
    }

    @JsonProperty("reclamationPercentage")
    public float getReclamationPercentage() {
        return reclamationPercentage;
    }

    @JsonProperty("reclamationPercentage")
    public void setReclamationPercentage(float reclamationPercentage) {
        this.reclamationPercentage = reclamationPercentage;
    }
}
