package org.graalvm.argo.lambda_manager.core;

import java.io.File;

public class StartupHook implements Runnable {

    @Override
    public void run() {
        purgeDirectory(new File(Environment.CODEBASE), false);
        purgeDirectory(new File(Environment.LAMBDA_LOGS), false);
        purgeDirectory(new File(Environment.MANAGER_LOGS), false);
        purgeDirectory(new File(Environment.MANAGER_METRICS), false);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void purgeDirectory(File dir, boolean deleteDir) {
        File[] files = dir.listFiles((dir1, name) -> !name.equals(".gitkeep"));
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    purgeDirectory(file, true);
                }
                file.delete();
            }
            if (deleteDir) {
                dir.delete();
            }
        }
    }
}
