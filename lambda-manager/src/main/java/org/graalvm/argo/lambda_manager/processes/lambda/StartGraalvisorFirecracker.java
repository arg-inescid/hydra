package org.graalvm.argo.lambda_manager.processes.lambda;

import java.util.List;

import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Lambda;

public class StartGraalvisorFirecracker extends StartFirecracker {

    public StartGraalvisorFirecracker(Lambda lambda) {
        super(lambda);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = prepareCommand("graalvisor");
        command.add(TIMESTAMP_TAG + System.currentTimeMillis());
        command.add(PORT_TAG + Configuration.argumentStorage.getLambdaPort());
        command.add("LD_LIBRARY_PATH=/lib:/lib64:/tmp/apps:/usr/local/lib");
        command.add("JAVA_HOME=/jvm");
        return command;
    }

}
