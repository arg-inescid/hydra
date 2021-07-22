package com.lambda_manager.processes.main;

import java.util.ArrayList;
import java.util.List;

public class RemoveTapsOutsidePool extends RemoveTapsFromPool {

    @Override
    protected List<String> makeCommand() {
        List<String> command = new ArrayList<>();
        command.add("bash");
        command.add("src/scripts/remove_remain_taps.sh");
        return command;
    }
}
