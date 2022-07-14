package org.graalvm.argo.lambda_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
                "scheduler",
                "encoder",
                "storage",
                "client",
                "codeWriter",
                "lambdaInfo"
})
public class LambdaManagerState implements Serializable {

    @JsonProperty("scheduler") private String scheduler;
    @JsonProperty("encoder") private String encoder;
    @JsonProperty("storage") private String storage;
    @JsonProperty("client") private String client;
    @JsonProperty("codeWriter") private String codeWriter;
    private final static long serialVersionUID = -5763918485183329321L;

    /**
     * No args constructor for use in serialization
     *
     */
    public LambdaManagerState() {
    }

    /**
     *
     * @param scheduler - The Scheduler class which will be used by the Manager.
     * @param encoder - The Encoder class which will be used by the Manager.
     * @param storage - The Lambda Storage class which will be used by the Manager.
     * @param client - The Client class which will be used by the Manager.
     * @param codeWriter - The CodeWriter class which will be used by the Manager.
     */
    public LambdaManagerState(String scheduler, String optimizer, String encoder, String storage, String client,
                    String codeWriter) {
        super();
        this.scheduler = scheduler;
        this.encoder = encoder;
        this.storage = storage;
        this.client = client;
        this.codeWriter = codeWriter;
    }

    @JsonProperty("scheduler")
    public String getScheduler() {
        return scheduler;
    }

    @JsonProperty("scheduler")
    public void setScheduler(String scheduler) {
        this.scheduler = scheduler;
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

}