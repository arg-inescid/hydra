package org.graalvm.argo.lambda_manager.processes.devmapper;

import org.graalvm.argo.lambda_manager.processes.AbstractProcess;
import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.ArrayList;
import java.util.List;

public class DeleteDevmapperBase extends AbstractProcess {

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/devmapper/delete_base_images.sh");
        return command;
    }

    @Override
    protected String outputFilename() {
        return Environment.DELETE_DEVMAPPER_BASE_FILENAME;
    }
}
