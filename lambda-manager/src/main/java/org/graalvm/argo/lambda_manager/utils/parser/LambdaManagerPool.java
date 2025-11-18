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
                "hydra",
                "graalos",
                "customJava",
                "customJavaScript",
                "customPython",
                "hydraPgo",
                "hydraPgoOptimized"
})
public class LambdaManagerPool implements Serializable {
    @JsonProperty("hotspotWithAgent") private int hotspotWithAgent;
    @JsonProperty("hotspot") private int hotspot;
    @JsonProperty("hydra") private int hydra;
    @JsonProperty("graalos") private int graalos;
    @JsonProperty("customJava") private int customJava;
    @JsonProperty("customJavaScript") private int customJavaScript;
    @JsonProperty("customPython") private int customPython;
    @JsonProperty("hydraPgo") private int hydraPgo;
    @JsonProperty("hydraPgoOptimized") private int hydraPgoOptimized;
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
     * @param hydra - Maximum number of Hydra lambdas in the pool.
     * @param custom - Maximum number of custom (OpenWhisk) lambdas in the pool.
     */
    public LambdaManagerPool(int hotspotWithAgent, int hotspot, int hydra, int graalos, int customJava, int customJavaScript, int customPython, int hydraPgo, int hydraPgoOptimized) {
        super();
        this.hotspotWithAgent = hotspotWithAgent;
        this.hotspot = hotspot;
        this.hydra = hydra;
        this.graalos = graalos;
        this.customJava = customJava;
        this.customJavaScript = customJavaScript;
        this.customPython = customPython;
        this.hydraPgo = hydraPgo;
        this.hydraPgoOptimized = hydraPgoOptimized;
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

    @JsonProperty("hydra")
    public int getHydra() {
        return hydra;
    }

    @JsonProperty("hydra")
    public void setHydra(int hydra) {
        this.hydra = hydra;
    }

    @JsonProperty("graalos")
    public void setGraalOS(int graalos) {
        this.graalos = graalos;
    }

    @JsonProperty("graalos")
    public int getGraalOS() {
        return graalos;
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

    @JsonProperty("hydraPgo")
    public int getHydraPgo() {
        return hydraPgo;
    }

    @JsonProperty("hydraPgo")
    public void setHydraPgo(int hydraPgo) {
        this.hydraPgo = hydraPgo;
    }

    @JsonProperty("hydraPgoOptimized")
    public int getHydraPgoOptimized() {
        return hydraPgoOptimized;
    }

    @JsonProperty("hydraPgoOptimized")
    public void setHydraPgoOptimized(int hydraPgoOptimized) {
        this.hydraPgoOptimized = hydraPgoOptimized;
    }
}
