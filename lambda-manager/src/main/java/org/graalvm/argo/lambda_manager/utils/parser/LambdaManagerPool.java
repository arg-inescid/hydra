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
                "customJava",
                "customJavaScript",
                "customPython",
                "graalvisorPgo",
                "graalvisorPgoOptimized"
})
public class LambdaManagerPool implements Serializable {
    @JsonProperty("hotspotWithAgent") private int hotspotWithAgent;
    @JsonProperty("hotspot") private int hotspot;
    @JsonProperty("graalvisor") private int graalvisor;
    @JsonProperty("customJava") private int customJava;
    @JsonProperty("customJavaScript") private int customJavaScript;
    @JsonProperty("customPython") private int customPython;
    @JsonProperty("graalvisorPgo") private int graalvisorPgo;
    @JsonProperty("graalvisorPgoOptimized") private int graalvisorPgoOptimized;
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
    public LambdaManagerPool(int hotspotWithAgent, int hotspot, int graalvisor, int customJava, int customJavaScript, int customPython, int graalvisorPgo, int graalvisorPgoOptimized) {
        this.hotspotWithAgent = hotspotWithAgent;
        this.hotspot = hotspot;
        this.graalvisor = graalvisor;
        this.customJava = customJava;
        this.customJavaScript = customJavaScript;
        this.customPython = customPython;
        this.graalvisorPgo = graalvisorPgo;
        this.graalvisorPgoOptimized = graalvisorPgoOptimized;
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

    @JsonProperty("customJava")
    public int getCustomJava() {
        return customJava;
    }

    @JsonProperty("customJava")
    public void setCustomJava(int customJava) {
        this.customJava = customJava;
    }

    @JsonProperty("customJavaScript")
    public int getCustomJavaScript() {
        return customJavaScript;
    }

    @JsonProperty("customJavaScript")
    public void setCustomJavaScript(int customJavaScript) {
        this.customJavaScript = customJavaScript;
    }

    @JsonProperty("customPython")
    public int getCustomPython() {
        return customPython;
    }

    @JsonProperty("customPython")
    public void setCustomPython(int customPython) {
        this.customPython = customPython;
    }

    @JsonProperty("graalvisorPgo")
    public int getGraalvisorPgo() {
        return graalvisorPgo;
    }

    @JsonProperty("graalvisorPgo")
    public void setGraalvisorPgo(int graalvisorPgo) {
        this.graalvisorPgo = graalvisorPgo;
    }

    @JsonProperty("graalvisorPgoOptimized")
    public int getGraalvisorPgoOptimized() {
        return graalvisorPgoOptimized;
    }

    @JsonProperty("graalvisorPgoOptimized")
    public void setGraalvisorPgoOptimized(int graalvisorPgoOptimized) {
        this.graalvisorPgoOptimized = graalvisorPgoOptimized;
    }
}
