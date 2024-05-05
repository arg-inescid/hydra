package org.graalvm.argo.lambda_manager.processes.lambda;

import org.graalvm.argo.lambda_manager.core.Lambda;

import java.util.List;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;

public class StartGraalvisorPgoContainer extends StartContainer {

    public StartGraalvisorPgoContainer(Lambda lambda) {
        super(lambda);
    }

    protected List<String> makeCommand() {
        List<String> command = prepareCommand("graalvisor:latest");
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add("LD_LIBRARY_PATH=/lib:/lib64:/tmp/apps:/usr/local/lib");
        return command;
    }
}
