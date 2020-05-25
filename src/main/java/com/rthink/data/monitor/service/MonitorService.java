package com.rthink.data.monitor.service;

public interface MonitorService {
	
	boolean isCurrChannelId(String message);

	void monitor(String message);
}
