package com.lambda_manager.utils.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringParsingJSONFile;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringSerializationJSONObject;
import com.lambda_manager.utils.Messages;
import com.lambda_manager.utils.logger.Logger;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Level;

public class ArgumentParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static LambdaManagerConfiguration parse(String configData) throws ErrorDuringParsingJSONFile {
        try {
            return objectMapper.readValue(configData, LambdaManagerConfiguration.class);
        } catch (IOException ioException) {
            throw new ErrorDuringParsingJSONFile(Messages.ERROR_PARSING_JSON, ioException);
        }
    }

    public static void serialize(LambdaManagerConfiguration lambdaManagerConfiguration, OutputStreamWriter writer)
            throws ErrorDuringSerializationJSONObject {
        try {
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapper.writeValue(writer, lambdaManagerConfiguration);
            Logger.log(Level.INFO, String.format(Messages.SUCCESS_SERIALIZE_JSON, writer));
        } catch (IOException ioException) {
            throw new ErrorDuringSerializationJSONObject(Messages.ERROR_SERIALIZE_JSON, ioException);
        }
    }
}