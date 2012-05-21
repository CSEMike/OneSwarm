package edu.washington.cs.oneswarm.f2f.servicesharing;



import java.util.Collections;
import java.util.List;

public class UrlToServer {
	List<ServerPublicInfo> servers;
	
	public UrlToServer(ServerPublicInfo[] servers){
		for(ServerPublicInfo server : servers)
			addServer(server);
	}
	
	public void addServer(ServerPublicInfo server){
		servers.add(server);
		Collections.sort(servers);
	}
	
	public ServerPublicInfo pickServer(String url, int port){
		for(ServerPublicInfo server : servers)
			if(server.allowsConnectionTo(url, port))
				return server;
		return null;
	}
}
