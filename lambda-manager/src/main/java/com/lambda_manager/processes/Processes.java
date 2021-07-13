package com.lambda_manager.processes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO - move to utils.
public class Processes {

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
