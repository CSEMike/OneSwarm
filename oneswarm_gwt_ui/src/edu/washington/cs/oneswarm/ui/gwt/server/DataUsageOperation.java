package edu.washington.cs.oneswarm.ui.gwt.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.Strings;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentList;


public class DataUsageOperation {
	private String[] stateInfo = {"0","0.0","0","0.0","0","0.0","0","0.0","0","0","0","0","0","0","0","0"};
	private boolean firstExecution = true;
	private int numberStopped = 0;
	private TorrentInfo[] torrents;
	private CoreInterface coreint;
	private Stopper stopper; 
	private int oldupload;
	private int olddownload;
	private DataUsageWriter datawrite = null;
	
	public DataUsageOperation() {
	}
	
	public boolean getStopped() {
		if (stopper != null) {
			return stopper.getStopped();
		} else {
			return false;
		}
	}
	
	public void setCore(CoreInterface coreinterface) {
		coreint = coreinterface;
	}
	
	public HashMap<String, String> getLimits() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(Strings.SIDEBAR_DAILYLIMIT, stateInfo[1] + "");
		map.put(Strings.SIDEBAR_WEEKLYLIMIT, stateInfo[3] + "");
		map.put(Strings.SIDEBAR_MONTHLYLIMIT, stateInfo[5] + "");
		map.put(Strings.SIDEBAR_YEARLYLIMIT, stateInfo[7] + "");
		return map;
	}
	
	public HashMap<String, String> getCounts() {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(Strings.SIDEBAR_DAYCOUNT, stateInfo[0] + "");
		map.put(Strings.SIDEBAR_WEEKCOUNT, stateInfo[2] + "");
		map.put(Strings.SIDEBAR_MONTHCOUNT, stateInfo[4] + "");
		map.put(Strings.SIDEBAR_YEARCOUNT, stateInfo[6] + "");
		return map;
	}
	
	public HashMap<String, String> getStats(TorrentList torrent) {
		
		HashMap<String, String> map = null;
		map = new HashMap<String, String>();
		try {
			map.put(Strings.SIDEBAR_DAYCOUNT, "0");
			map.put(Strings.SIDEBAR_WEEKCOUNT, "0");
			map.put(Strings.SIDEBAR_MONTHCOUNT, "0");
			map.put(Strings.SIDEBAR_YEARCOUNT, "0");
			map.put(Strings.SIDEBAR_DAILYLIMIT, "0");
			map.put(Strings.SIDEBAR_WEEKLYLIMIT, "0");
			map.put(Strings.SIDEBAR_MONTHLYLIMIT, "0");
			map.put(Strings.SIDEBAR_YEARLYLIMIT, "0");
			AzureusCore core = AzureusCoreImpl.getSingleton();

			if (AzureusCoreImpl.isCoreAvailable() == false)
				return map;   

			if(torrent == null){
				return map;
			}
			torrents = torrent.getTorrentInfos();
			
			org.gudy.azureus2.core3.global.GlobalManagerStats stats = core.getGlobalManager().getStats();
			
			long totalTransfer = stats.getTotalDataBytesReceived() + stats.getTotalDataBytesSent();
			File transferStorage = new File(SystemProperties.getUserPath() + File.separator + "usagestatsdata.txt");
			Calendar current = Calendar.getInstance();
			long lastTransfer = 0;
			int thisday = current.get(Calendar.DAY_OF_YEAR);
			int thisdaybackup = thisday;
			int thisyear = current.get(Calendar.YEAR);
			int lastday = 0;
			int lastyear = 0;
			int dayofweek = current.get(Calendar.DAY_OF_WEEK);
			int dayofmonth = current.get(Calendar.DAY_OF_MONTH);
			long lastDayTransfer = 0;
			String dataString = "";
			String limitString = "";
			String warningString = "";
			ArrayList<String> dataArray = new ArrayList<String>();
			String[] limitArray = {"0.0", "0.0", "0.0", "0.0"};
			String[] warningArray = {"0","0","0","0","0","0","0","0"};
			if (transferStorage.exists()) {
				Scanner input = new Scanner(transferStorage);
				if (input.hasNext()) {
					lastTransfer = Long.parseLong(input.next());
				}
				if (input.hasNext()) {
					dataString = input.next();
				}
				if (input.hasNext()) {
					limitString = input.next();
				}
				if (input.hasNext()) {
					warningString = input.next();
				}
				dataArray = new ArrayList<String>(Arrays.asList(dataString.split(",")));
				limitArray = limitString.split(",");
				warningArray = warningString.split(",");
				if (dataArray.size() >= 3){
					lastday = Integer.parseInt(dataArray.get(dataArray.size()-3));
					lastyear = Integer.parseInt(dataArray.get(dataArray.size()-2));
					lastDayTransfer = Long.parseLong(dataArray.get(dataArray.size()-1));
				} else {
					throw new Exception("dataArray should have at least 3 values");
				}
				if (lastyear > thisyear || (!(lastyear < thisyear) && lastday > thisday)) {
					transferStorage.delete();
					for (int n = 0;n < stateInfo.length;n++) {
						stateInfo[n] = "0";
					}
					return map;
				} else {
					while (lastyear < thisyear) {
						lastday++;
						while (lastday < yearLength(thisyear)) {
							dataArray.add(lastday + "");
							dataArray.add(lastyear + "");
							dataArray.add("0");
							lastday++;
						}
						lastyear++;
						lastday = 1;
						dataArray.clear();
						dataArray.add(lastday + "");
						dataArray.add(lastyear + "");
						dataArray.add("0");
					}
					while (lastday < thisday) {
						lastday++;
						lastDayTransfer = 0;
						dataArray.add(lastday + "");
						dataArray.add(lastyear + "");
						dataArray.add("0");
					}
					if (lastyear == thisyear && lastday == thisday) {
						if (firstExecution) {
							stopper = new Stopper(torrents, coreint);
							stopper.run();
							if (datawrite == null) {
								datawrite = new DataUsageWriter(totalTransfer + "\n" + thisday + "," + thisyear + "," + totalTransfer + "\n" + "0.0,0.0,0.0,0.0" + "\n" + "0,0,0,0,0,0,0,0", transferStorage);
							}
							lastTransfer = 0;
							stateInfo[1] = limitArray[0];
							stateInfo[3] = limitArray[1];
							stateInfo[5] = limitArray[2];
							stateInfo[7] = limitArray[3];
							for (int u = 0; u < 8;u++) {
								stateInfo[u+8] = warningArray[u];
							}
						} else {
							totalTransfer -= lastTransfer;
						}	
						lastDayTransfer += totalTransfer;
						stateInfo[0] = lastDayTransfer+"";
						dataArray.set(dataArray.size()-1, lastDayTransfer + "");
					} 
					String output = "";
					output += (totalTransfer + lastTransfer + "\n");
					for (int j = 0;j < dataArray.size()-1;j++) {
						output += (dataArray.get(j) + ",");
					}
					output += (dataArray.get(dataArray.size()-1) + "\n");
					for (int k = 0;k < 3;k++) {
						output += (stateInfo[(k*2)+1] + ",");
					}
					output += (stateInfo[7] + "\n");
					for (int h = 0;h < 7;h++) {
						output += (stateInfo[h+8] + ",");
					}
					output += (stateInfo[15] + "\n");
					datawrite.updateData(output);
					if (firstExecution) {
						if (!datawrite.isRunning()) {
							datawrite.run();
						}
						firstExecution = false;
					}
					stopper.updateFields(torrents, coreint);
				}
			} else {
				transferStorage.createNewFile();
				datawrite = new DataUsageWriter(totalTransfer + "\n" + thisday + "," + thisyear + "," + totalTransfer + "\n" + "0.0,0.0,0.0,0.0" + "\n" + "0,0,0,0,0,0,0,0", transferStorage); 
				datawrite.run();
				map.put(Strings.SIDEBAR_DAYCOUNT, "0");
				map.put(Strings.SIDEBAR_WEEKCOUNT, "0");
				map.put(Strings.SIDEBAR_MONTHCOUNT, "0");
				map.put(Strings.SIDEBAR_YEARCOUNT, "0");
				map.put(Strings.SIDEBAR_DAILYLIMIT, "0");
				map.put(Strings.SIDEBAR_WEEKLYLIMIT, "0");
				map.put(Strings.SIDEBAR_MONTHLYLIMIT, "0");
				map.put(Strings.SIDEBAR_YEARLYLIMIT, "0");
				map.put("Stopped", "Running");
				return map;
			}
			long weeklyamount = getTransfer(dataArray, dayofweek);
			long monthlyamount = getTransfer(dataArray, dayofmonth);
			long yearlyamount = getTransfer(dataArray, thisdaybackup);
			stateInfo[2] = weeklyamount+"";
			stateInfo[4] = monthlyamount+"";
			stateInfo[6] = yearlyamount+"";
			String stopped = checkLimits(dataArray, weeklyamount, monthlyamount, yearlyamount);
			map.put(Strings.SIDEBAR_DAYCOUNT, dataArray.get(dataArray.size()-1) + "");
			map.put(Strings.SIDEBAR_WEEKCOUNT, weeklyamount + "");
			map.put(Strings.SIDEBAR_MONTHCOUNT, monthlyamount + "");
			map.put(Strings.SIDEBAR_YEARCOUNT, yearlyamount + "");
			map.put(Strings.SIDEBAR_DAILYLIMIT, stateInfo[1] + "");
			map.put(Strings.SIDEBAR_WEEKLYLIMIT, stateInfo[3] + "");
			map.put(Strings.SIDEBAR_MONTHLYLIMIT, stateInfo[5] + "");
			map.put(Strings.SIDEBAR_YEARLYLIMIT, stateInfo[7] + "");
			map.put("Stopped", stopped);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}
	
	public String[] checkWarning() {
		String[] returnArray = new String[32];
		returnArray[0] = "false"; //bool, category, value, limit, bool category, value, limit
		returnArray[4] = "false";
		returnArray[8] = "false";
		returnArray[12] = "false";
		returnArray[16] = "false";
		returnArray[20] = "false";
		returnArray[24] = "false";
		returnArray[28] = "false";
				String[] warningArray = {stateInfo[8], stateInfo[9], stateInfo[10], stateInfo[11], stateInfo[12], stateInfo[13], stateInfo[14], stateInfo[15]};
				for (int i = 0;i < 4;i++) {
					if (!warningArray[i+4].equals("1")) {
						if (Double.parseDouble(stateInfo[(i*2)+1]) != 0) {
							if (Long.parseLong(stateInfo[i*2]) >= Double.parseDouble(stateInfo[(i*2)+1])*1073741824) {
								warningArray[i+4] = "1";
	 							returnArray[4+(8*i)] = "true";
								if (i==0) {
									returnArray[5+(8*i)] = "daily";
								} else if (i==1) {
									returnArray[5+(8*i)] = "weekly";
								} else if (i==2) {
									returnArray[5+(8*i)] = "monthly";
								} else {
									returnArray[5+(8*i)] = "yearly";
								}
								returnArray[7+(8*i)] = StringTools.formatRate(stateInfo[(i*2)]);
								returnArray[6+(8*i)] = stateInfo[(i*2)+1]+" GB";
								warningArray[i+4] = "1";
							} else {
								warningArray[i+4] = "0";
							}
						} else {
							warningArray[i+4] = "0";
						}
					} else if ((Long.parseLong(stateInfo[i*2]) < Double.parseDouble(stateInfo[(i*2)+1])*1073741824*(0.9)) || (Double.parseDouble(stateInfo[(i*2)+1]) == 0)) {
						warningArray[i+4] = "0";
					}
					if (!warningArray[i].equals("1")) {
						if (Double.parseDouble(stateInfo[(i*2)+1]) != 0) {
							if (Long.parseLong(stateInfo[i*2]) >= Double.parseDouble(stateInfo[(i*2)+1])*1073741824*(0.9)) {
								warningArray[i] = "1";
	 							returnArray[0+(8*i)] = "true";
								if (i==0) {
									returnArray[1+(8*i)] = "daily";
								} else if (i==1) {
									returnArray[1+(8*i)] = "weekly";
								} else if (i==2) {
									returnArray[1+(8*i)] = "monthly";
								} else {
									returnArray[1+(8*i)] = "yearly";
								}
								returnArray[3+(8*i)] = StringTools.formatRate(stateInfo[(i*2)]);
								returnArray[2+(8*i)] = stateInfo[(i*2)+1]+" GB";
								warningArray[i] = "1";
							} else {
								warningArray[i] = "0";
							}
						} else {
							warningArray[i] = "0";
						}
					} else if ((Long.parseLong(stateInfo[i*2]) < Double.parseDouble(stateInfo[(i*2)+1])*1073741824*(0.9)) || (Double.parseDouble(stateInfo[(i*2)+1]) == 0)) {
						warningArray[i] = "0";
						warningArray[i+4] = "0";
					} 
				}
				stateInfo[8] = warningArray[0];
				stateInfo[9] = warningArray[1];
				stateInfo[10] = warningArray[2];
				stateInfo[11] = warningArray[3];
				stateInfo[12] = warningArray[4];
				stateInfo[13] = warningArray[5];
				stateInfo[14] = warningArray[6];
				stateInfo[15] = warningArray[7];
				return returnArray;
	}
	
	public void resetLimit(String limittype) {
			if (limittype.equals("daily")) {
				stateInfo[1] = "0.0";
			} else if (limittype.equals("weekly")) {
				stateInfo[3] = "0.0";
			} else if (limittype.equals("monthly")) {
				stateInfo[5] = "0.0";
			} else {
				stateInfo[7] = "0.0";
			}
	}
	
	public void setLimits(String day, String week, String month, String year) {	
		System.out.println("Set limits called");
		stateInfo[1] = day;
		stateInfo[3] = week;
		stateInfo[5] = month;
		stateInfo[7] = year;
	}
	
	private long getTransfer(ArrayList<String> dataArray, int amountofdays) {
		long totaltransfer = 0;
		int subtraction = 1;
		while (amountofdays > 0) {
			if (dataArray.size() < subtraction) {
				break;
			}
			totaltransfer += Long.parseLong(dataArray.get(dataArray.size()-subtraction));
			subtraction+=3;
			amountofdays--;
		}
		return totaltransfer;
	}
	
	private int yearLength(int year) {
		if (((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0) ) {
			return 366;
		} else {
			return 365;
		}
	}
	
	private String checkLimits(ArrayList<String> dataArray, long weeklyamount, long monthlyamount, long yearlyamount) {
		double dayLimit = Double.parseDouble(stateInfo[1]); 
		double weekLimit = Double.parseDouble(stateInfo[3]);
		double monthLimit = Double.parseDouble(stateInfo[5]);
		double yearLimit = Double.parseDouble(stateInfo[7]);
		if (dayLimit > 0) {
			if (Long.parseLong(dataArray.get(dataArray.size()-1)) >= dayLimit*1073741824) {
				numberStopped++;
			} 
		} 
		if (weekLimit > 0) {
			if (weeklyamount >= weekLimit*1073741824) {
				numberStopped++;
			} 
		} 
		if (monthLimit > 0) {
			if (monthlyamount >= monthLimit*1073741824) {
				numberStopped++;
			} 
			
		} 
		if (yearLimit > 0) {
			if (yearlyamount >= yearLimit*1073741824) {
				numberStopped++;
			}
		}
		if (numberStopped > 0) {
			numberStopped = 0;
			if (!stopper.getStopped()) {
				oldupload = COConfigurationManager.getIntParameter("Max Upload Speed KBs");
				olddownload = COConfigurationManager.getIntParameter("Max Download Speed KBs");
			}
			System.out.println("Stopped");
			stopper.setStopped(true);
			AzureusCoreImpl.getSingleton().getPluginManager().getPluginInterfaceByClass(DHTPlugin.class).setDisabled(true);
			COConfigurationManager.setParameter("Max Upload Speed KBs", 1);
			COConfigurationManager.setParameter("Max Download Speed KBs", 1);
			ConfigurationManager.getInstance().setDirty();
			return "Stopped";
		} else {
			numberStopped = 0;
			if (!firstExecution) {
				if (stopper.getStopped()) {
					System.out.println("REVERTINGRATES: " + oldupload + " " + olddownload);
					COConfigurationManager.setParameter("Max Upload Speed KBs", oldupload);
					COConfigurationManager.setParameter("Max Download Speed KBs", olddownload);
					ConfigurationManager.getInstance().setDirty();
				}
				stopper.setStopped(false);
				AzureusCoreImpl.getSingleton().getPluginManager().getPluginInterfaceByClass(DHTPlugin.class).setDisabled(false);
			}
			return "Running";
		}
	}
}