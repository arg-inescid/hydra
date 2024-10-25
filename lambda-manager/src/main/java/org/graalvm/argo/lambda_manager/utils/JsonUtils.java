package org.graalvm.argo.lambda_manager.utils;

import java.util.logging.Level;

import org.graalvm.argo.lambda_manager.utils.logger.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String convertParametersIntoJsonObject(String arguments, String entryPoint, String functionName) {
        return convertParametersIntoJsonObject(arguments, entryPoint, functionName, false, null, 0, 0);
    }

    public static String convertParametersIntoJsonObject(String arguments, String entryPoint, String functionName, boolean debug, String requestId, long currentTimestamp, long lmTimestamp) {
        ObjectNode inputObject = mapper.createObjectNode();

        if (arguments != null) {
            inputObject.put("arguments", arguments);
        }

        if (entryPoint != null) {
            inputObject.put("entryPoint", entryPoint);
        }

        if (functionName != null) {
            inputObject.put("name", functionName);
        }

        if (debug) {
            inputObject.put("debug", "true");
        }

        if (requestId != null) {
            inputObject.put("requestId", requestId);
        }

        if (currentTimestamp != 0) {
            inputObject.put("sendTimestamp", currentTimestamp);
        }

        if (lmTimestamp != 0) {
            inputObject.put("lmTimestamp", lmTimestamp);
        }

        String resultJSON = "";
        try {
            resultJSON = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(inputObject);
        } catch (JsonProcessingException jsonProcessingException) {
            Logger.log(Level.SEVERE, jsonProcessingException.getMessage(), jsonProcessingException);
        }
        return resultJSON;
    }

    public static String constructJsonResponseObject(Object response) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode resultObject = mapper.createObjectNode();
            if (response instanceof String) {
                resultObject.put("data", (String) response);
            } else if (response instanceof JsonNode) {
                resultObject.set("data", (JsonNode) response);
            }
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultObject);
        } catch (Throwable throwable) {
            Logger.log(Level.SEVERE, throwable.getMessage(), throwable);
            return Messages.INTERNAL_ERROR;
        }
    }
}
