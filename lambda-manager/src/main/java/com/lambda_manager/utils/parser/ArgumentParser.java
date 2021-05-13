package com.lambda_manager.utils.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringParsingJSONFile;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringSerializationJSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArgumentParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static LambdaManagerConfiguration parse(String configData) throws ErrorDuringParsingJSONFile {
        try {
            return objectMapper.readValue(configData, LambdaManagerConfiguration.class);
        } catch (IOException ioException) {
            throw new ErrorDuringParsingJSONFile("Error during parsing JSON configuration file!", ioException);
        }
    }

    public static void serialize(LambdaManagerConfiguration lambdaManagerConfiguration, OutputStreamWriter writer)
            throws ErrorDuringSerializationJSONObject {
        try {
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapper.writeValue(writer, lambdaManagerConfiguration);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO, "Serialized object is: \n" + writer);
        } catch (IOException ioException) {
            throw new ErrorDuringSerializationJSONObject("Error during serialization of JSON object!", ioException);
        }
    }
}