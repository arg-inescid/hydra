package org.graalvm.argo.graalvisor.engine;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;

public class PolyglotEngineSingletonFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        // This code runs during image generation.
        ImageSingletons.add(PolyglotEngine.class, new PolyglotEngine());
    }
}