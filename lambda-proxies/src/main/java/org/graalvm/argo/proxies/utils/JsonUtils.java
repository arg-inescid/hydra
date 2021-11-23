package org.graalvm.argo.proxies.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jr.ob.JSON;

public class JsonUtils {

    public static final JSON json = JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT);

    /**
     * Extract arguments json encoded string into Map.
     *
     * @param jsonString json encoded String
     * @return map that illustrates json
     */
    public static Map<String, Object> jsonToMap(String jsonString) {
        try {
            if (jsonString.length() > 0)
                return json.mapFrom(jsonString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    private static Object processValue(Value value, ObjectMapper mapper) {
        if (value.hasMembers()) {
            ObjectNode object = mapper.createObjectNode();
            for (String key : value.getMemberKeys()) {
                Value innerValue = value.getMember(key);
                if (innerValue.hasMembers()) {
                    object.set(key, (JsonNode) processValue(innerValue, mapper));
                } else {
                    object.put(key, (String) processValue(innerValue, mapper));
                }
            }
            return object;
        } else {
            return value.toString();
        }
    }

    public static String valueToJson(Value value) {
        String result;
        if (value.hasMembers()) {
            ObjectMapper mapper = new ObjectMapper();
            Object json = processValue(value, mapper);
            try {
                result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            } catch (JsonProcessingException e) {
                result = "Exception occurred during JSON object processing";
            }
        } else {
            result = "Return value should be a dictionary-like object";
        }
        return result;
    }

}
