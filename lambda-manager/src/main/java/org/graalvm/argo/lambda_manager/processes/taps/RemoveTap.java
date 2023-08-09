package org.graalvm.argo.lambda_manager.processes.taps;

import org.graalvm.argo.lambda_manager.processes.AbstractProcess;
import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.ArrayList;
import java.util.List;

public class RemoveTap extends AbstractProcess {

    private final String tap;

    public RemoveTap(String tap) {
        this.tap = tap;
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/remove_taps.sh");
        command.add(tap);
        return command;
    }

    @Override
    protected String outputFilename() {
        return Environment.REMOVE_TAPS_FILENAME;
    }
}
