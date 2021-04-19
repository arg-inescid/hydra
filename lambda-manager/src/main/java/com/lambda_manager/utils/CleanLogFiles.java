package com.lambda_manager.utils;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationStartupEvent;

import javax.inject.Singleton;
import java.io.File;
import java.io.FilenameFilter;

@SuppressWarnings("unused")
@Singleton
public class CleanLogFiles implements ApplicationEventListener<ApplicationStartupEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartupEvent event) {
        purgeDirectory(new File("src/lambdas"), false);
        purgeDirectory(new File("src/logs/taps"), false);
        purgeDirectory(new File("src/logs/managers"), false);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void purgeDirectory(File dir, boolean deleteDir) {
        File[] files = dir.listFiles((dir1, name) -> !name.equals(".gitkeep"));
        if(files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    purgeDirectory(file, true);
                }
                file.delete();
            }
            if(deleteDir) {
                dir.delete();
            }
        }
    }
}
