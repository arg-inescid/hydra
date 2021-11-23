package org.graalvm.argo.lambda_manager.core;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationStartupEvent;

import javax.inject.Singleton;
import java.io.File;

@SuppressWarnings("unused")
@Singleton
public class StartupHook implements ApplicationEventListener<ApplicationStartupEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartupEvent event) {
        purgeDirectory(new File(Environment.CODEBASE), false);
        purgeDirectory(new File(Environment.LAMBDA_LOGS), false);
        purgeDirectory(new File(Environment.MANAGER_LOGS), false);
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
