package org.graalvm.argo.lambda_manager.processes.taps;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.graalvm.argo.lambda_manager.utils.LambdaConnection;

public class RemoveTapsOutsidePool extends RemoveTapsFromPool {

    public RemoveTapsOutsidePool(Queue<LambdaConnection> connections) {
        super(connections);
    }

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/remove_remain_taps.sh");
        return command;
    }
}
