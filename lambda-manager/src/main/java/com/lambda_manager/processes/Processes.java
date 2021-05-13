package com.lambda_manager.processes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.lambda_manager.processes.main.BuildNativeImage;
import com.lambda_manager.processes.main.CreateTaps;
import com.lambda_manager.processes.main.RemoveTapsFromPool;
import com.lambda_manager.processes.main.RemoveTapsOutsidePool;
import com.lambda_manager.processes.start_lambda.*;
import com.lambda_manager.processes.start_lambda.impl.StartHotspot;
import com.lambda_manager.processes.start_lambda.impl.StartHotspotWithAgent;
import com.lambda_manager.processes.start_lambda.impl.StartNativeImage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Processes {
    public static final AbstractProcess CREATE_TAPS = new CreateTaps();
    public static final AbstractProcess REMOVE_TAPS_FROM_POOL = new RemoveTapsFromPool();
    public static final AbstractProcess REMOVE_TAPS_OUTSIDE_POOL = new RemoveTapsOutsidePool();
    public static final AbstractProcess BUILD_NATIVE_IMAGE = new BuildNativeImage();

    public static final AbstractProcess START_LAMBDA = new StartLambda();
    public static final AbstractProcess START_HOTSPOT = new StartHotspot();
    public static final AbstractProcess START_HOTSPOT_WITH_AGENT = new StartHotspotWithAgent();
    public static final AbstractProcess START_NATIVE_IMAGE = new StartNativeImage();
    
    public static void printProcessErrorStream(Logger logger, Level level, Process p) throws IOException {
		BufferedReader is = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line;

	    while ((line = is.readLine()) != null) {
	    	logger.log(level, line);
	    }
    }
    
    public static void printProcessInputStream(Logger logger, Level level, Process p) throws IOException {
		BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;

	    while ((line = is.readLine()) != null) {
	    	logger.log(level, line);
	    }
    }
}
