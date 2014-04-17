package com.alangeorge.minermonitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ServiceThreadFactory implements ThreadFactory {
	
	@Override
	public Thread newThread(Runnable r) {
		ThreadFactory wrappedFactory = Executors.defaultThreadFactory();
		
		Thread thread = wrappedFactory.newThread(r);
		
		thread.setName("MinerUpdateServiceThread");
		
		return thread;
	}

}
