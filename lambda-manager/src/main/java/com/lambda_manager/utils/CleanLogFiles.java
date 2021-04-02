package com.lambda_manager.utils;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationStartupEvent;

import javax.inject.Singleton;
import java.io.File;
import java.util.Objects;

@SuppressWarnings("unused")
@Singleton
public class CleanLogFiles implements ApplicationEventListener<ApplicationStartupEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartupEvent event) {
        File lambdasDir = new File("src/lambdas");
        for (File lambdaDir : Objects.requireNonNull(lambdasDir.listFiles())) {
            for (File logDir : Objects.requireNonNull(lambdaDir.listFiles(pathname -> pathname.getName().contains("logs")))) {
                for (File file : Objects.requireNonNull(logDir.listFiles())) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        }
    }
}
