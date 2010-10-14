package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;

public class VersionCheckAdvancedDialog extends OneSwarmDialogBox {
	private static final int WIDTH = 400;
	
	Button okButton = new Button(msg.button_save());
	Button cancelButton = new Button(msg.button_cancel());
	
	CheckBox betaCheckbox = new CheckBox(msg.settings_interface_version_check_advanced());
	
	public VersionCheckAdvancedDialog() {
		super(false, true, false);
		
		setText(msg.settings_interface_version_check_advanced_header());
		
		Label selectLabel = new Label(msg.settings_interface_version_check_advanced_msg());
		selectLabel.addStyleName(CSS_DIALOG_HEADER);
		selectLabel.setWidth(WIDTH + "px");
		
		VerticalPanel panel = new VerticalPanel();

		panel.add(selectLabel);
		
		panel.setWidth(WIDTH+"px");
		
		panel.add(betaCheckbox);
		
		betaCheckbox.setEnabled(false);

		HorizontalPanel buttons = new HorizontalPanel();
		buttons.add(cancelButton);
		buttons.add(okButton);
		buttons.setSpacing(3);
		
		okButton.addClickHandler(this);
		cancelButton.addClickHandler(this);
		
		panel.add(buttons);
		
		panel.setCellHorizontalAlignment(buttons, HorizontalPanel.ALIGN_RIGHT);
		
		this.setWidget(panel);
		
		OneSwarmRPCClient.getService().getBooleanParameterValue(OneSwarmRPCClient.getSessionID(), "oneswarm.beta.updates", new AsyncCallback<Boolean>(){
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
				hide();
			}

			public void onSuccess(Boolean result) {
				betaCheckbox.setValue(result);
				betaCheckbox.setEnabled(true);
			}});
	}
	
	public void onClick( ClickEvent sender ) {
		if( sender.getSource().equals(okButton) ) {
			OneSwarmRPCClient.getService().setBooleanParameterValue(OneSwarmRPCClient.getSessionID(), "oneswarm.beta.updates", betaCheckbox.getValue(), new AsyncCallback<Void>(){
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}

				public void onSuccess(Void result) {
					;
				}});
			hide();
		} else if( sender.getSource().equals(cancelButton) ) {
			hide();
		} else {
			super.onClick(sender);
		}
	}
}
