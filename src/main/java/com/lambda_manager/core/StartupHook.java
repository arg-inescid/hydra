package com.lambda_manager.core;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationStartupEvent;

import javax.inject.Singleton;
import java.io.File;

import static com.lambda_manager.core.Environment.*;

@SuppressWarnings("unused")
@Singleton
public class StartupHook implements ApplicationEventListener<ApplicationStartupEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartupEvent event) {
        purgeDirectory(new File(CODEBASE), false);
        purgeDirectory(new File(LAMBDA_LOGS), false);
        purgeDirectory(new File(MANAGER_LOGS), false);
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
