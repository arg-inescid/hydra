package com.lambda_manager.processes;

import com.lambda_manager.processes.main.BuildNativeImage;
import com.lambda_manager.processes.main.CreateTap;
import com.lambda_manager.processes.main.RemoveTaps;
import com.lambda_manager.processes.start_lambda.*;
import com.lambda_manager.processes.start_lambda.impl.StartHotspot;
import com.lambda_manager.processes.start_lambda.impl.StartHotspotWithAgent;
import com.lambda_manager.processes.start_lambda.impl.StartHotspotWithBuildNativeImage;
import com.lambda_manager.processes.start_lambda.impl.StartNativeImage;

public class Processes {
    public static final AbstractProcess CREATE_TAP = new CreateTap();
    public static final AbstractProcess REMOVE_TAPS = new RemoveTaps();
    public static final AbstractProcess BUILD_NATIVE_IMAGE = new BuildNativeImage();

    public static final AbstractProcess START_LAMBDA = new StartLambda();
    public static final AbstractProcess START_HOTSPOT = new StartHotspot();
    public static final AbstractProcess START_HOTSPOT_WITH_AGENT = new StartHotspotWithAgent();
    public static final AbstractProcess START_HOTSPOT_WITH_BUILD = new StartHotspotWithBuildNativeImage();
    public static final AbstractProcess START_NATIVE_IMAGE = new StartNativeImage();
}
