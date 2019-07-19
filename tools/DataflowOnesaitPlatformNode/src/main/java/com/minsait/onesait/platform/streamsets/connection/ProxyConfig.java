package com.minsait.onesait.platform.streamsets.connection;

import lombok.Getter;

@Getter
public class ProxyConfig {
	private String host;
	private int port;
	private String user;
	private String password;
	
	public ProxyConfig(String host, int port, String user, String password){
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
	}
}
