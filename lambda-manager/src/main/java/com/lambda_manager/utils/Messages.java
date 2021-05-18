package com.lambda_manager.utils;

public class Messages {

    // General messages.
    public static final String INTERNAL_ERROR = "Internal server error!";
    public static final String HTTP_TIMEOUT = "HTTP request timeout!";
    public static final String LOG_REDIRECTION = "Log is redirected to file -> %s";
    public static final String UNDELIVERABLE_EXCEPTION = "Undeliverable exception received!";

    // Lambda Manager configuration messages.
    public static final String NO_CONFIGURATION_UPLOADED = "No configuration has been uploaded!";
    public static final String SUCCESS_CONFIGURATION_UPLOAD = "Successfully uploaded lambda manager configuration!";
    public static final String ERROR_CONFIGURATION_UPLOAD = "Error during uploading new configuration!";

    // Upload function messages.
    public static final String SUCCESS_FUNCTION_UPLOAD = "Successfully uploaded function - %s!";
    public static final String ERROR_FUNCTION_UPLOAD = "Error during uploading new function - %s!";

    // Remove function messages.
    public static final String SUCCESS_FUNCTION_REMOVE = "Successfully removed function - %s!";

    // Request messages.
    public static final String FUNCTION_NOT_FOUND = "Function - %s - has not been uploaded!";
    public static final String TIME_HOTSPOT_W_AGENT = "Time (hotspot-w-agent_id=%d): %d\t[ms]";
    public static final String TIME_HOTSPOT = "Time (hotspot_id=%d): %d\t[ms]";
    public static final String TIME_NATIVE_IMAGE = "Time (vmm_id=%d): %d\t[ms]";

    // Tap messages.
    public static final String ERROR_TAP_REMOVAL = "Error during cleaning taps!";

    // Process messages.
    public static final String PROCESS_START = "PID -> %d | Command -> %s | Output -> %s";
    public static final String PROCESS_EXIT = "PID -> %d | Command -> %s | Exit code -> %d";
    public static final String PROCESS_RAISE_EXCEPTION = "PID -> %d | Command -> %s | Raised exception!";

    // Connection pool messages.
    public static final String ERROR_POOL_CREATION = "Error during creating new connection pool!";

    // Parse/Serialize JSON.
    public static final String ERROR_PARSING_JSON = "Error during parsing JSON configuration file!";
    public static final String ERROR_SERIALIZE_JSON = "Error during serialization of JSON object!";
    public static final String SUCCESS_SERIALIZE_JSON = "Serialized object is: %n%s";


}
