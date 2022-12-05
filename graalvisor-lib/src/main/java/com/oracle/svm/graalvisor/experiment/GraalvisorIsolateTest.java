package com.oracle.svm.graalvisor.experiment;

import java.io.IOException;

import com.oracle.svm.graalvisor.api.GraalVisorAPI;
import com.oracle.svm.graalvisor.types.GuestIsolateThread;

public class GraalvisorIsolateTest {

	public static void main(String[] args) throws IOException {
		GraalVisorAPI api = new GraalVisorAPI("/home/serhii/experiment/hwapp");
		GuestIsolateThread isolateThread = api.createIsolate();
		String result = api.invokeFunction(isolateThread, "com.hello_world.HelloWorld", "");
		System.out.println(result);
		api.tearDownIsolate(isolateThread);
		api.close();
	}

}
