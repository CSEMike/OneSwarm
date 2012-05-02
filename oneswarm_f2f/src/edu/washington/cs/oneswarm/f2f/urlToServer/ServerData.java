package edu.washington.cs.oneswarm.f2f.urlToServer;

import java.util.Set;

public class ServerData {
	String id;
	boolean useWhitelist;
	Set<String> blacklist;
	int avgPing;
	
	public ServerData(String id) {
		this.id = id;
	}
}
