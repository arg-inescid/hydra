package com.oracle.svm.graalvisor.utils;

import static com.oracle.svm.graalvisor.api.AsGraalVisorHost.graalvisorHostSystemProperty;
import static com.oracle.svm.graalvisor.guestapi.AsGraalVisorGuest.graalvisorGuestSystemProperty;

import javax.naming.ConfigurationException;

public class BuildOptionUtils {
    public static void checkValidity() {
        if (Boolean.getBoolean(graalvisorHostSystemProperty) && Boolean.getBoolean(graalvisorGuestSystemProperty)) {
            try {
                throw new ConfigurationException("Only one of GraalVisorHost and GraalVisorGuest system property can be defined.");
            } catch (ConfigurationException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
