package com.lambda_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "instance",
        "list"
})
public class LambdaInfo implements Serializable
{

    @JsonProperty("instance")
    private String instance;
    @JsonProperty("list")
    private String list;
    private final static long serialVersionUID = 4416228688532741447L;

    /**
     * No args constructor for use in serialization
     *
     */
    public LambdaInfo() {
    }

    /**
     *
     * @param instance - The class which collects information about one instance of the lambda.
     * @param list - The class which collects information about multiple instances of the same lambda.
     */
    public LambdaInfo(String instance, String list) {
        super();
        this.instance = instance;
        this.list = list;
    }

    @JsonProperty("instance")
    public String getInstance() {
        return instance;
    }

    @JsonProperty("instance")
    public void setInstance(String instance) {
        this.instance = instance;
    }

    @JsonProperty("list")
    public String getList() {
        return list;
    }

    @JsonProperty("list")
    public void setList(String list) {
        this.list = list;
    }

}