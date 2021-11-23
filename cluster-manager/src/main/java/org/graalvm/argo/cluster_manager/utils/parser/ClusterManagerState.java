package org.graalvm.argo.cluster_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"scheduler", "storage"})
public class ClusterManagerState implements Serializable {

    @JsonProperty("scheduler") private String scheduler;
    @JsonProperty("storage") private String storage;
    private final static long serialVersionUID = -5763918485183329321L;

    /**
     * No args constructor for use in serialization
     *
     */
    public ClusterManagerState() {
    }

    /**
     *
     * @param scheduler - The Scheduler class which will be used by the Manager.
     * @param storage - The Lambda Storage class which will be used by the Manager.
     */
    public ClusterManagerState(String scheduler, String storage) {
        super();
        this.scheduler = scheduler;
        this.storage = storage;
    }

    @JsonProperty("scheduler")
    public String getScheduler() {
        return scheduler;
    }

    @JsonProperty("scheduler")
    public void setScheduler(String scheduler) {
        this.scheduler = scheduler;
    }

    @JsonProperty("storage")
    public String getStorage() {
        return storage;
    }

    @JsonProperty("storage")
    public void setStorage(String storage) {
        this.storage = storage;
    }
}