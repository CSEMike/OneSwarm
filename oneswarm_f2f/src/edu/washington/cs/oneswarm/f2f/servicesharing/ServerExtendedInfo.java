package edu.washington.cs.oneswarm.f2f.servicesharing;



import java.util.Date;
import java.util.Queue;

public class ServerExtendedInfo extends ServerPublicInfo{
	// Measured in kilobytes (rounded)
	private Queue<Integer> bandwidthHistory; //Last 10 reported bandwidths
	private int avgBandwidth;
	private Queue<Integer> latencyHistory; //Last 10 reported latencies
	private int avgLatency;
	
	private static final int HISTORY_LENGTH = 10;
	
	public ServerExtendedInfo(String nickname, String ip, String id, int advertBandwidth, String[] exitPolicy, Date lastOutage, String version){
		super(nickname, ip, id, advertBandwidth, exitPolicy, lastOutage, version);
	}
	
	public int compareTo(ServerPublicInfo other){
		if(other instanceof ServerExtendedInfo){
			return this.avgBandwidth - ((ServerExtendedInfo) other).avgBandwidth;
		}
		return super.compareTo(other);
	}
	
	public void recordBandwidth(int kbps){
		bandwidthHistory.add(kbps);
		avgBandwidth = averageIntQueue(bandwidthHistory);
	}
	
	public void recordLatency(int ms){
		latencyHistory.add(ms);
		avgLatency = averageIntQueue(latencyHistory);
	}
	
	public int getAvgBandwidth(){
		return avgBandwidth;
	}
	
	public int getAvgLatency(){
		return avgLatency;
	}
	
	private int averageIntQueue(Queue<Integer> q){
		while(q.size() > HISTORY_LENGTH)
			q.remove();
		int sum = 0;
		for(int i = 0; i < q.size(); i++)
			sum += q.remove();
		return sum / q.size();
	}
}
