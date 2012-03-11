package org.gudy.azureus2.ui.swt.views.clientstats;

import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.util.BEncodableObject;

import com.aelitis.azureus.util.MapUtils;

public class ClientStatsDataSource
	implements BEncodableObject
{

	public String client;

	public int count;

	public int current;

	public long bytesReceived;

	public long bytesDiscarded;

	public long bytesSent;

	public ClientStatsOverall overall;

	public ClientStatsDataSource() {
	}

	public ClientStatsDataSource(Map loadMap) {
		client = MapUtils.getMapString(loadMap, "client", "?");
		count = MapUtils.getMapInt(loadMap, "count", 0);
		bytesReceived = MapUtils.getMapLong(loadMap, "bytesReceived", 0);
		bytesDiscarded = MapUtils.getMapLong(loadMap, "bytesDiscarded", 0);
		bytesSent = MapUtils.getMapLong(loadMap, "bytesSent", 0);
	}

	public Object toBencodeObject() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("client", client);
		map.put("count", Long.valueOf(count));
		map.put("bytesReceived", Long.valueOf(bytesReceived));
		map.put("bytesDiscarded", Long.valueOf(bytesDiscarded));
		map.put("bytesSent", Long.valueOf(bytesSent));
		return map;
	}
}
