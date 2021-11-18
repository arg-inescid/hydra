package com.cluster_manager.core;

import java.net.MalformedURLException;
import java.net.URL;

import io.micronaut.context.BeanContext;
import io.micronaut.http.client.RxHttpClient;

public class WorkerManager {

	private static final int WORKER_MANAGER_PORT = 9000;
	
	private final String address;
	private RxHttpClient client;
	
	public WorkerManager(String address) {
		this.address = address;
		
	}

	public String getAddress() {
		return address;
	}
	
	public synchronized RxHttpClient getClient(BeanContext beanContext) throws MalformedURLException {
		if (client == null) {
			client = beanContext.createBean(RxHttpClient.class, new URL("http", address, WORKER_MANAGER_PORT, "/"));
		}
		return client;
	}
}
