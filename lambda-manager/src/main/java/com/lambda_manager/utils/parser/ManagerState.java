package com.lambda_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "scheduler",
        "optimizer",
        "encoder",
        "storage",
        "client",
        "codeWriter",
        "lambdaInfo"
})
public class ManagerState implements Serializable {

    @JsonProperty("scheduler")
    private String scheduler;
    @JsonProperty("optimizer")
    private String optimizer;
    @JsonProperty("encoder")
    private String encoder;
    @JsonProperty("storage")
    private String storage;
    @JsonProperty("client")
    private String client;
    @JsonProperty("codeWriter")
    private String codeWriter;
    @JsonProperty("lambdaInfo")
    private LambdaInfo lambdaInfo;
    private final static long serialVersionUID = -5763918485183329321L;

    /**
     * No args constructor for use in serialization
     *
     */
    public ManagerState() {
    }

    /**
     *
     * @param scheduler - The Scheduler class which will be used by the Manager.
     * @param optimizer - The Optimizer class which will be used by the Manager.
     * @param client - The Client class which will be used by the Manager.
     * @param storage - The Lambda Storage class which will be used by the Manager.
     * @param encoder - The Encoder class which will be used by the Manager.
     * @param lambdaInfo - The class which stores information about one type of the lambda,
     */
    public ManagerState(String scheduler, String optimizer, String encoder, String storage, String client, String codeWriter, LambdaInfo lambdaInfo) {
        super();
        this.scheduler = scheduler;
        this.optimizer = optimizer;
        this.encoder = encoder;
        this.storage = storage;
        this.client = client;
        this.codeWriter = codeWriter;
        this.lambdaInfo = lambdaInfo;
    }

    @JsonProperty("scheduler")
    public String getScheduler() {
        return scheduler;
    }

    @JsonProperty("scheduler")
    public void setScheduler(String scheduler) {
        this.scheduler = scheduler;
    }

    @JsonProperty("optimizer")
    public String getOptimizer() {
        return optimizer;
    }

    @JsonProperty("optimizer")
    public void setOptimizer(String optimizer) {
        this.optimizer = optimizer;
    }

    @JsonProperty("encoder")
    public String getEncoder() {
        return encoder;
    }

    @JsonProperty("encoder")
    public void setEncoder(String encoder) {
        this.encoder = encoder;
    }

    @JsonProperty("storage")
    public String getStorage() {
        return storage;
    }

    @JsonProperty("storage")
    public void setStorage(String storage) {
        this.storage = storage;
    }

    @JsonProperty("client")
    public String getClient() {
        return client;
    }

    @JsonProperty("client")
    public void setClient(String client) {
        this.client = client;
    }

    @JsonProperty("codeWriter")
    public String getCodeWriter() {
        return codeWriter;
    }

    @JsonProperty("codeWriter")
    public void setCodeWriter(String codeWriter) {
        this.codeWriter = codeWriter;
    }

    @JsonProperty("lambdaInfo")
    public LambdaInfo getLambdaInfo() {
        return lambdaInfo;
    }

    @JsonProperty("lambdaInfo")
    public void setLambdaInfo(LambdaInfo lambdaInfo) {
        this.lambdaInfo = lambdaInfo;
    }

}