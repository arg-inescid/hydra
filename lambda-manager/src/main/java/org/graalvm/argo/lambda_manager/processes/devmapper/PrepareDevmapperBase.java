package org.graalvm.argo.lambda_manager.processes.devmapper;

import org.graalvm.argo.lambda_manager.processes.AbstractProcess;
import org.graalvm.argo.lambda_manager.core.Configuration;
import org.graalvm.argo.lambda_manager.core.Environment;

import java.util.ArrayList;
import java.util.List;

public class PrepareDevmapperBase extends AbstractProcess {

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/devmapper/prepare_base_images.sh");
        // VM images are different with snapshotting enabled and without.
        if (Configuration.argumentStorage.isSnapshotEnabled()) {
            command.add("snapshot");
        }
        return command;
    }

    @Override
    protected String outputFilename() {
        return Environment.PREPARE_DEVMAPPER_BASE_FILENAME;
    }
}
