package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

abstract class SettingsPanel extends VerticalPanel
{
	protected final static OSMessages msg = OneSwarmGWT.msg;

	private boolean ready_save = false;
	
	public boolean isReadyToSave() { return ready_save; }
	
	void loadNotify()
	{
		ready_save = true;
	}
	
	abstract public void sync();
	
	abstract String validData();
}
