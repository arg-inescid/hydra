package com.cluster_manager.utils;

public class Messages {

    // General messages.
    public static final String INTERNAL_ERROR = "Internal server error!";
    public static final String HTTP_TIMEOUT = "HTTP request timeout!";
    public static final String LOG_REDIRECTION = "Log is redirected to file -> %s";
    public static final String UNDELIVERABLE_EXCEPTION = "Undeliverable exception received!";

    // Lambda Manager configuration messages.
    public static final String NO_CONFIGURATION_UPLOADED = "No configuration has been uploaded!";
    public static final String CONFIGURATION_ALREADY_UPLOADED = "Configuration has been already uploaded!";
    public static final String SUCCESS_CONFIGURATION_UPLOAD = "Successfully uploaded lambda manager configuration!";
    public static final String ERROR_CONFIGURATION_UPLOAD = "Error during uploading new configuration!";

    // Upload function messages.
    public static final String SUCCESS_FUNCTION_UPLOAD = "Successfully uploaded function - %s!";
    public static final String ERROR_FUNCTION_LANG = "Function language not found - %s!";
    public static final String ERROR_FUNCTION_UPLOAD = "Error during uploading new function - %s!";
    
    // Remove function messages.
    public static final String SUCCESS_FUNCTION_REMOVE = "Successfully removed function - %s!";
    public static final String ERROR_FUNCTION_REMOVE = "Error during removing function - %s!";
    
    // Parse/Serialize JSON.
    public static final String ERROR_PARSING_JSON = "Error during parsing JSON configuration file!";
    public static final String ERROR_SERIALIZE_JSON = "Error during serialization of JSON object!";
    public static final String SUCCESS_SERIALIZE_JSON = "Serialized object is: %n%s";

}
