package com.lambda_manager.utils.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringParsingJSONFile;
import com.lambda_manager.exceptions.argument_parser.ErrorDuringSerializationJSONObject;
import com.lambda_manager.exceptions.argument_parser.InvalidJSONFile;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ArgumentParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ManagerArguments parse(String configData) throws InvalidJSONFile, ErrorDuringParsingJSONFile {
        try {
            return objectMapper.readValue(configData, ManagerArguments.class);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            throw new ErrorDuringParsingJSONFile("Error during parsing JSON configuration file!", ioException);
        }
    }

    public static void serialize(ManagerArguments managerArguments) throws ErrorDuringSerializationJSONObject {
        try {
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            StringWriter writer = new StringWriter();
            objectMapper.writeValue(writer, managerArguments);
            System.out.println("Serialized object is: \n" + writer);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            throw new ErrorDuringSerializationJSONObject("Error during serialization of JSON object!", ioException);
        }
    }

    public static void dummy(ManagerArguments managerArguments) {
        try {
            Class<?> clazz = Class.forName(managerArguments.getManagerState().getLambdaInfo().getInstance());
            Constructor<?> constructor = clazz.getConstructor();
            Object newInstance = constructor.newInstance();
            LambdaInstanceInfo lambdaInstanceInfo = (LambdaInstanceInfo) newInstance;
            System.out.println("LambdaInstanceInfo " + lambdaInstanceInfo);

            clazz = Class.forName(managerArguments.getManagerState().getLambdaInfo().getList());
            constructor = clazz.getConstructor();
            newInstance = constructor.newInstance();
            LambdaInstancesInfo lambdaInstancesInfo = (LambdaInstancesInfo) newInstance;
            System.out.println("LambdaInstancesInfo " + lambdaInstancesInfo);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}