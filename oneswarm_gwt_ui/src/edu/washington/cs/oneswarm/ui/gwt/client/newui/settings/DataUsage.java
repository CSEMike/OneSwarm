package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.HashMap;
import java.util.Map;


import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Button;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.Strings;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;

public class DataUsage extends SettingsPanel implements Updateable {
	private final Label thisday = new Label(": 0 B");
	private final Label thisweek = new Label(": 0 B");
	private final Label thismonth = new Label(": 0 B");
	private final Label thisyear = new Label(": 0 B");
	private final TextBox monthlylimit = new TextBox();
	private final TextBox dailylimit = new TextBox();
	private final TextBox weeklylimit = new TextBox();
	private final TextBox yearlylimit = new TextBox();
	private long mNextUpdate = 0;
	
	DataUsage() {
		loadNotify();
		HorizontalPanel panel = new HorizontalPanel();
		panel.setWidth("340px");
		Grid g = new Grid(5, 5);
		g.setWidget(0, 3, new Label("Daily Limit*  :"));
		g.setWidget(1, 3, new Label("Weekly Limit* :"));
		g.setWidget(2, 3, new Label("Monthly Limit*:"));
		g.setWidget(3, 3, new Label("Yearly Limit* :"));
		g.setWidget(0, 0, new Label("Today"));
		g.setWidget(1, 0, new Label("This Week"));
		g.setWidget(2, 0, new Label("This Month"));
		g.setWidget(3, 0, new Label("This Year"));
		g.setWidget(0, 1, thisday);
		g.setWidget(1, 1, thisweek);
		g.setWidget(2, 1, thismonth);
		g.setWidget(3, 1, thisyear);
		g.setWidget(0, 4, dailylimit);
		g.setWidget(1, 4, weeklylimit);
		g.setWidget(2, 4, monthlylimit);
		g.setWidget(3, 4, yearlylimit);
		Label subinfo = new Label("*In Gigabytes, set 0 for unlimited");
		subinfo.setHorizontalAlignment(ALIGN_CENTER);
		dailylimit.setWidth("30px");
		weeklylimit.setWidth("30px");
		monthlylimit.setWidth("30px");
		yearlylimit.setWidth("30px");
		dailylimit.setMaxLength(8);
		weeklylimit.setMaxLength(8);
		monthlylimit.setMaxLength(8);
		yearlylimit.setMaxLength(8);
		OneSwarmRPCClient.getService().getLimits(OneSwarmRPCClient.getSessionID(), new AsyncCallback<HashMap<String, String>>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
				
			}
			public void onSuccess(HashMap<String, String> result) {
				if (!(result.get(Strings.SIDEBAR_DAILYLIMIT).equals("") && result.get(Strings.SIDEBAR_WEEKLYLIMIT).equals("") && result.get(Strings.SIDEBAR_MONTHLYLIMIT).equals("") && result.get(Strings.SIDEBAR_YEARLYLIMIT).equals(""))) {
					dailylimit.setText(result.get(Strings.SIDEBAR_DAILYLIMIT) + "");
					weeklylimit.setText(result.get(Strings.SIDEBAR_WEEKLYLIMIT) + "");
					monthlylimit.setText(result.get(Strings.SIDEBAR_MONTHLYLIMIT) + "");
					yearlylimit.setText(result.get(Strings.SIDEBAR_YEARLYLIMIT) + "");
				}
			}
		});
		super.add(panel);
		super.add(g);
		super.add(subinfo);
	}
	
	@Override
	public void sync() {	
		if (!dailylimit.getText().matches("([0-9]+\\Q.\\E?[0-9]+)|([0-9]*)")){
			dailylimit.setText("0.0");
		}
		if (!weeklylimit.getText().matches("([0-9]+\\Q.\\E?[0-9]+)|([0-9]*)")){
			weeklylimit.setText("0.0");
		}
		if (!monthlylimit.getText().matches("([0-9]+\\Q.\\E?[0-9]+)|([0-9]*)")){
			monthlylimit.setText("0.0");
		}
		if (!yearlylimit.getText().matches("([0-9]+\\Q.\\E?[0-9]+)|([0-9]*)")){
			yearlylimit.setText("0.0");
		}
		OneSwarmRPCClient.getService().setLimits(OneSwarmRPCClient.getSessionID(), dailylimit.getText(), weeklylimit.getText(), monthlylimit.getText(), yearlylimit.getText(), new AsyncCallback<String>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}
			public void onSuccess(String result) {
				System.out.println("Updated Limits to following: day: " + dailylimit.getText() + " week: " + weeklylimit.getText() + " month: " + monthlylimit.getText() + " year: " + yearlylimit.getText());
			}
		});
	}

	@Override
	String validData() {
		return null;
	}
	
	public void onDetach() {
		super.onDetach();
		OneSwarmGWT.removeFromUpdateTask(this);
	}

	public void onAttach() {
		super.onAttach();
		OneSwarmGWT.addToUpdateTask(this);
	}

	public void update(int count) {
		if (mNextUpdate < System.currentTimeMillis()) {
			mNextUpdate = Long.MAX_VALUE;

			OneSwarmRPCClient.getService().getCounts(OneSwarmRPCClient.getSessionID(), new AsyncCallback<HashMap<String, String>>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
					mNextUpdate = System.currentTimeMillis() + 5000;
				}

				public void onSuccess(HashMap<String, String> result) {
					
					//System.out.println("result has: " + result.size());
					
					if (result != null) {
						if (!(result.get(Strings.SIDEBAR_DAYCOUNT).equals("0") && result.get(Strings.SIDEBAR_WEEKCOUNT).equals("0") && result.get(Strings.SIDEBAR_MONTHCOUNT).equals("0") && result.get(Strings.SIDEBAR_YEARCOUNT).equals("0"))) {
							thisday.setText(": " + StringTools.formatRate(result.get(Strings.SIDEBAR_DAYCOUNT)));
							thisweek.setText(": " + StringTools.formatRate(result.get(Strings.SIDEBAR_WEEKCOUNT)));
							thismonth.setText(": " + StringTools.formatRate(result.get(Strings.SIDEBAR_MONTHCOUNT)));
							thisyear.setText(": " + StringTools.formatRate(result.get(Strings.SIDEBAR_YEARCOUNT)));
						}
					}
					mNextUpdate = System.currentTimeMillis() + 1000;
				}
			});
		}
	}
}
