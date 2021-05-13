package com.lambda_manager.handlers;

import com.lambda_manager.collectors.lambda_info.LambdaInstanceInfo;
import com.lambda_manager.collectors.lambda_info.LambdaInstancesInfo;
import com.lambda_manager.core.LambdaManager;
import com.lambda_manager.processes.ProcessBuilder;
import com.lambda_manager.processes.Processes;
import com.lambda_manager.utils.LambdaTuple;

import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultLambdaShutdownHandler extends TimerTask {

    private final LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda;
    private final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public DefaultLambdaShutdownHandler(LambdaTuple<LambdaInstancesInfo, LambdaInstanceInfo> lambda) {
        this.lambda = lambda;
    }

    private void shutdownHotSpotLambda(String lambdaPath) throws Throwable {
		Process p = new java.lang.ProcessBuilder(new String[]{"bash", "src/scripts/stop_hotspot.sh", lambdaPath}).start();
		p.waitFor();
		if (p.exitValue() != 0) {
			logger.log(Level.WARNING, String.format("Lambda ID=%d failed to terminate successfuly", lambda.instance.getId()));
			Processes.printProcessErrorStream(logger, Level.WARNING, p);
		}
    }
    
    private void shutdownLambda() {
    	try {
    		
	    	switch(lambda.instance.getExecutionMode()) {
	        case HOTSPOT:
	        	shutdownHotSpotLambda(String.format("src/codebase/%s/start-hotspot-id-%d", lambda.list.getName(), lambda.instance.getId()));
	        	break;
	        case HOTSPOT_W_AGENT:
	        	shutdownHotSpotLambda(String.format("src/codebase/%s/start-hotspot-w-agent-id-%d", lambda.list.getName(), lambda.instance.getId()));
				break;
			case NATIVE_IMAGE:
				// Currently we don't shutdown lambdas running in Native Image.
				break;
			default:
				logger.log(Level.WARNING, String.format("Lambda ID=%d has no known execution mode: %s", lambda.instance.getId(), lambda.instance.getExecutionMode()));
				return;
	        }
	    	
            
    	} catch (Throwable t) {
    		logger.log(Level.SEVERE, String.format("Lambda ID=%d failed to shutdown: %s", lambda.instance.getId(), t.getMessage()));
    		t.printStackTrace();
    	}
    }
    
    @Override
    public void run() {
        ProcessBuilder processBuilder;

        synchronized (lambda.list) {
            if(!lambda.list.getStartedInstances().remove(lambda.instance)) {
                return;
            }
            lambda.instance.getTimer().cancel();
            processBuilder = lambda.list.getCurrentlyActiveWorkers().remove(lambda.instance.getId());
        }
        
        shutdownLambda();
        processBuilder.shutdownInstance();

        // Cleanup is finished, add server back to list of all available servers.
        synchronized (lambda.list) {
            LambdaManager.getConfiguration().argumentStorage.returnConnectionTriplet(lambda.instance.getConnectionTriplet());
            lambda.list.getAvailableInstances().add(lambda.instance);
            lambda.list.notify();
        }
    }
}
