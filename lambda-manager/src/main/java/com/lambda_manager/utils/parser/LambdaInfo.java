package com.lambda_manager.utils.parser;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "lambda",
        "function"
})
public class LambdaInfo implements Serializable
{

    @JsonProperty("lambda")
    private String lambda;
    @JsonProperty("function")
    private String function;
    private final static long serialVersionUID = 4416228688532741447L;

    /**
     * No args constructor for use in serialization
     *
     */
    public LambdaInfo() {
    }

    /**
     *
     * @param lambda - The class which collects information about the lambdas.
     * @param function - The class which collects information about the functions.
     */
    public LambdaInfo(String lambda, String function) {
        super();
        this.lambda = lambda;
        this.function = function;
    }

    @JsonProperty("lambda")
    public String getLambda() {
        return lambda;
    }

    @JsonProperty("lambda")
    public void setLambda(String lambda) {
        this.lambda = lambda;
    }

    @JsonProperty("function")
    public String getFunction() {
        return function;
    }

    @JsonProperty("function")
    public void setFunction(String function) {
        this.function = function;
    }

}