package org.graalvm.argo.lambda_manager.processes.taps;

import org.graalvm.argo.lambda_manager.processes.AbstractProcess;
import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.ArrayList;
import java.util.List;

public class CreateTap extends AbstractProcess {

    private final String tap;

    public CreateTap(String tap) {
        this.tap = tap;
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/create_taps.sh");
        command.add(tap);
        return command;
    }

    @Override
    protected String outputFilename() {
        return Environment.CREATE_TAPS_FILENAME;
    }
}
