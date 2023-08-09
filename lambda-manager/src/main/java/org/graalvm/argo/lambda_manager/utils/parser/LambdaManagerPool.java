package org.graalvm.argo.lambda_manager.utils.parser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serializable;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
                "hotspotWithAgent",
                "hotspot",
                "graalvisor",
                "custom"
})
public class LambdaManagerPool implements Serializable {
    @JsonProperty("hotspotWithAgent") private int hotspotWithAgent;
    @JsonProperty("hotspot") private int hotspot;
    @JsonProperty("graalvisor") private int graalvisor;
    @JsonProperty("custom") private int custom;
    private final static long serialVersionUID = -620888442421704577L;

    /**
     * No args constructor for use in serialization
     *
     */
    public LambdaManagerPool() {
    }

    /**
     * @param hotspotWithAgent - Maximum number of HotSpot with Agent lambdas in the pool.
     * @param hotspot - Maximum number of HotSpot lambdas in the pool.
     * @param graalvisor - Maximum number of Graalvisor lambdas in the pool.
     * @param custom - Maximum number of custom (OpenWhisk) lambdas in the pool.
     */
    public LambdaManagerPool(int hotspotWithAgent, int hotspot, int graalvisor, int custom) {
        super();
        this.hotspotWithAgent = hotspotWithAgent;
        this.hotspot = hotspot;
        this.graalvisor = graalvisor;
        this.custom = custom;
    }

    @JsonProperty("hotspotWithAgent")
    public int getHotspotWithAgent() {
        return hotspotWithAgent;
    }

    @JsonProperty("hotspotWithAgent")
    public void setHotspotWithAgent(int hotspotWithAgent) {
        this.hotspotWithAgent = hotspotWithAgent;
    }

    @JsonProperty("hotspot")
    public int getHotspot() {
        return hotspot;
    }

    @JsonProperty("hotspot")
    public void setHotspot(int hotspot) {
        this.hotspot = hotspot;
    }

    @JsonProperty("graalvisor")
    public int getGraalvisor() {
        return graalvisor;
    }

    @JsonProperty("graalvisor")
    public void setGraalvisor(int graalvisor) {
        this.graalvisor = graalvisor;
    }

    @JsonProperty("custom")
    public int getCustom() {
        return custom;
    }

    @JsonProperty("custom")
    public void setCustom(int custom) {
        this.custom = custom;
    }

}
