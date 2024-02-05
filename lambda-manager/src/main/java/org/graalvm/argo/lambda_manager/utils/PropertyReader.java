package org.graalvm.argo.lambda_manager.utils;

import java.io.InputStream;
import java.util.Properties;

public class PropertyReader {


    private static final String PROPERTIES_FILE = "application.yml";

    private final Properties properties;

    public PropertyReader() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                System.out.println("Unable to find " + PROPERTIES_FILE);
                return;
            }
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

}
