package org.graalvm.argo.lambda_manager.utils.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
                "turnOn",
                "redirectToFile",
                "fineGrain"
})
public class LambdaManagerConsole implements Serializable {
    @JsonProperty("turnOn") private boolean turnOn;
    @JsonProperty("redirectToFile") private boolean redirectToFile;
    @JsonProperty("fineGrain") private boolean fineGrain;
    private final static long serialVersionUID = -620888442421704577L;

    /**
     * No args constructor for use in serialization
     *
     */
    public LambdaManagerConsole() {
    }

    /**
     * @param turnOn - Turn On/Off logging.
     * @param redirectToFile - Should the logging be redirected to file or printed in console.
     * @param fineGrain - Fine or coarse grain logging.
     */
    public LambdaManagerConsole(boolean turnOn, boolean redirectToFile, boolean fineGrain) {
        super();
        this.turnOn = turnOn;
        this.redirectToFile = redirectToFile;
        this.fineGrain = fineGrain;
    }

    @JsonProperty("turnOn")
    public boolean isTurnOn() {
        return turnOn;
    }

    @JsonProperty("turnOn")
    public void setTurnOn(boolean turnOn) {
        this.turnOn = turnOn;
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