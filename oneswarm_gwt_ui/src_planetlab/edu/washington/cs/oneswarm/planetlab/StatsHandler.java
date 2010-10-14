package edu.washington.cs.oneswarm.planetlab;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.servlet.ServletHandler;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;

public class StatsHandler extends ServletHandler{
	private CoreInterface coreInterface;
	private AzureusCore core;

	private final Map<String, TimeSeriesData> path_to_data = new ConcurrentHashMap<String, TimeSeriesData>();
	
	enum TimeSeriesData { 
		UPLOAD("ul"), DOWNLOAD("dl");
		
		List<Number> timeSeriesData = (List<Number>) Collections.synchronizedList(new LinkedList<Number>());
		private String servletPath;
		
		TimeSeriesData( String servletPath ) { 
			this.servletPath = servletPath;
		}
		
		public void update( Number value ) {
			synchronized(timeSeriesData) {
				timeSeriesData.add(value);
				if( timeSeriesData.size() > 86400 ) { // 1 day at 1/sec
					timeSeriesData.remove((int)0);
				}
			}
		}
	};
	
	public StatsHandler(CoreInterface coreInterface) {
		this.coreInterface = coreInterface;
		this.core = AzureusCoreImpl.getSingleton();
		
		for( TimeSeriesData t : TimeSeriesData.values() ) {
			path_to_data.put(t.servletPath, t);
		}
		
		Timer t = new Timer("Stats grabber for StatsHandler", true);
		TimerTask ul_dl = new TimerTask() {
			public void run() {
				TimeSeriesData.UPLOAD.update(core.getGlobalManager().getStats().getDataAndProtocolSendRate()/1024);
				TimeSeriesData.DOWNLOAD.update(core.getGlobalManager().getStats().getDataAndProtocolReceiveRate()/1024);	
			}			
		};
		t.schedule(ul_dl, 1000, 1000);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		PrintStream out = new PrintStream(response.getOutputStream());
		String which = request.getParameter("which");
		if( which == null ) { 
			out.println("need type");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		TimeSeriesData series = path_to_data.get(which);
		if( series == null ) { 
			out.println("unknown type");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		/**
		 * both in seconds
		 */
		int howlong = 60;
		int granularity = 1;
		
		if( request.getParameter("duration") != null ) {
			howlong = Integer.parseInt(request.getParameter("duration"));
		}
		if( request.getParameter("granularity") != null ) {
			granularity = Integer.parseInt(request.getParameter("granularity"));
		}
		
		List<Number> sampled = new java.util.ArrayList<Number>();
		synchronized(series.timeSeriesData) {
			int offset = series.timeSeriesData.size() - howlong;
			for( int i=0; i<howlong/granularity; i++ ) {
				double acc = 0;
				for( int sample=0; sample<granularity; sample++ ) {
					int idx = offset + (i*granularity) + sample;
					if( idx > series.timeSeriesData.size() ) { 
						System.err.println("out of bounds val req: " + idx);
					} else if( idx < 0 ) {
						System.out.println("out of bounds val req: " + idx);
					} else {  
						acc += series.timeSeriesData.get(idx).doubleValue();
					}
				}
				sampled.add(Math.round(acc / (double)granularity));
			}
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		
		StringBuffer url = new StringBuffer();
		url.append("http://chart.apis.google.com/chart?" + 
				"chs=325x300" + 
				"&chd=t:");
		
		int xtic = 1;
		if( howlong > 300 ) { // 5 minutes -> minutes
			xtic = 60;
		} else if( howlong > 18000 ) { // 5 hours -> hours
			xtic = 3600;
		}
		
		for( int i=0; i<sampled.size()-1; i++ ) {
			url.append((i*granularity)/xtic+",");
		}
		url.append((sampled.size()*granularity)/xtic + "|");
		double maxVal = 0;
		for( int i=0; i<sampled.size()-1; i++ ) {
			Number val = sampled.get(i);
			if( val.doubleValue() > maxVal ) { 
				maxVal = val.doubleValue();
			}
			url.append(val + ",");
		}
		url.append(sampled.get(sampled.size()-1));
		
		url.append("&cht=lxy" + 
				   "&chxt=x,y" +
				   "&chds=0," + (sampled.size()*granularity)/xtic + ",0," + maxVal +  
				   "&chxr=0,0," + (sampled.size()*granularity)/xtic + "|1,0," + maxVal);
		
		System.out.println("url: " + url);
		
		HttpURLConnection conn = (HttpURLConnection) new URL(url.toString()).openConnection();
		byte [] buff = new byte[4*1024];
		InputStream in = conn.getInputStream();
		int read = 0;
		response.setContentType("image/png");
		while( (read = in.read(buff, 0, buff.length)) > 0 ) {
			response.getOutputStream().write(buff, 0, read);
		}
		response.flushBuffer();
	}		
}
