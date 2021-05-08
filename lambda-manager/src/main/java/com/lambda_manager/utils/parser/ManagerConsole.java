package com.lambda_manager.utils.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "turnOff",
        "redirectToFile",
        "fineGrain"
})
public class ManagerConsole implements Serializable
{
    @JsonProperty("turnOff")
    private boolean turnOff;
    @JsonProperty("redirectToFile")
    private boolean redirectToFile;
    @JsonProperty("fineGrain")
    private boolean fineGrain;
    private final static long serialVersionUID = -620888442421704577L;

    /**
     * No args constructor for use in serialization
     *
     */
    public ManagerConsole() {
    }

    /**
     * @param turnOff - Turn On/Off logging.
     * @param redirectToFile - Should the logging be redirected to file or printed in console.
     * @param fineGrain - Fine or coarse grain logging.
     */
    public ManagerConsole(boolean turnOff, boolean redirectToFile, boolean fineGrain) {
        super();
        this.turnOff = turnOff;
        this.redirectToFile = redirectToFile;
        this.fineGrain = fineGrain;
    }

    @JsonProperty("turnOff")
    public boolean isTurnOff() {
        return turnOff;
    }

    @JsonProperty("turnOff")
    public void setTurnOff(boolean turnOff) {
        this.turnOff = turnOff;
    }

    @JsonProperty("redirectToFile")
    public boolean isRedirectToFile() {
        return redirectToFile;
    }

    @JsonProperty("redirectToFile")
    public void setRedirectToFile(boolean redirectToFile) {
        this.redirectToFile = redirectToFile;
    }

    @JsonProperty("fineGrain")
    public boolean isFineGrain() {
        return fineGrain;
    }

    @JsonProperty("fineGrain")
    public void setFineGrain(boolean fineGrain) {
        this.fineGrain = fineGrain;
    }

}