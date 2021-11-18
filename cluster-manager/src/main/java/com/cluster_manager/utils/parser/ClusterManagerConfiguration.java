package com.cluster_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"workers", "managerState"})
public class ClusterManagerConfiguration implements Serializable {
	@JsonProperty("workers") private String workers;
	@JsonProperty("managerState") private ClusterManagerState clusterManagerState;
	private static final long serialVersionUID = 1L;

    /**
     * No args constructor for use in serialization
     *
     */
    public ClusterManagerConfiguration() {
    }

    /**
     *
     * @param clusterManagerState - The class that represent state of one manager's instance.
     */
    public ClusterManagerConfiguration(String workers, ClusterManagerState clusterManagerState) {
        super();
        this.clusterManagerState = clusterManagerState;
    }

    @JsonProperty("gateway")
    public String getWorkers() {
        return workers;
    }

    @JsonProperty("gateway")
    public void setWorkers(String workers) {
        this.workers = workers;
    }

    @JsonProperty("managerState")
    public ClusterManagerState getManagerState() {
        return clusterManagerState;
    }

    @JsonProperty("managerState")
    public void setManagerState(ClusterManagerState clusterManagerState) {
        this.clusterManagerState = clusterManagerState;
    }

}