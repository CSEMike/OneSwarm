package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.CommunityServerAddPanel;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;

public class CommunityServersSettingsPanel extends SettingsPanel implements ClickHandler {
	Button addButton = new Button(msg.button_add());
	Button propertiesButton = new Button(msg.button_edit());
	Button removeButton = new Button(msg.button_remove());
	ListBox currentList = new ListBox();
	
	//List<String> mServerList = new ArrayList<String>();
	List<CommunityRecord> currentRecs = new ArrayList<CommunityRecord>();
	
	public CommunityServersSettingsPanel() {
		HorizontalPanel topPanel = new HorizontalPanel();
		add(topPanel);
		setCellHorizontalAlignment(topPanel, VerticalPanel.ALIGN_LEFT);

		topPanel.setWidth("100%");
		currentList.setVisibleItemCount(3);

		addButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		removeButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
		propertiesButton.addStyleName(OneSwarmCss.SMALL_BUTTON);

		currentList.setWidth("100%");
		currentList.addItem(msg.loading());

		addButton.addClickHandler(this);
		removeButton.addClickHandler(this);
		propertiesButton.addClickHandler(this);

		HorizontalPanel hp = new HorizontalPanel();
		hp.add(currentList);
		hp.setCellWidth(currentList, "300px");
		// currentList.setWidth("300px");
		hp.setSpacing(3);
		VerticalPanel vp = new VerticalPanel();
		removeButton.setWidth("75px");
		addButton.setWidth("75px");
		propertiesButton.setWidth("75px");
		vp.setSpacing(3);
		vp.add(addButton);
		vp.add(removeButton);
		vp.add(propertiesButton);
		
		addButton.setEnabled(false);
		removeButton.setEnabled(false);
		propertiesButton.setEnabled(false);
		
		hp.add(vp);
		hp.setCellVerticalAlignment(vp, HorizontalPanel.ALIGN_MIDDLE);

		add(hp);
		this.setCellHorizontalAlignment(hp, HorizontalPanel.ALIGN_CENTER);
		
		OneSwarmRPCClient.getService().getStringListParameterValue(OneSwarmRPCClient.getSessionID(), "oneswarm.community.servers", new AsyncCallback<ArrayList<String>>(){
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
				System.err.println("oneswarm.community.servers settings get failed! " + caught.toString());
			}

			public void onSuccess(ArrayList<String> result) {
				
//				currentList.clear();
				
				if( (result.size() % 5) != 0 ) {
					System.err.println("Community servers doesn't conform to format");
					return;
				}
				
				Set<String> existing = new HashSet<String>();
				for( int i=0; i<result.size()/5; i++ ) {
					CommunityRecord rec = new CommunityRecord(result, 5*i);
					
					if( existing.contains(rec.getUrl()) ) {
						System.out.println("skipping duplicate url: " + rec.getUrl());
						continue;
					}
					
//					currentList.addItem(rec.getUrl());
					currentRecs.add(rec);
					
					existing.add(rec.getUrl());
				}
				
				refreshUI();
				
				addButton.setEnabled(true);
				removeButton.setEnabled(true);
				propertiesButton.setEnabled(true);
				
				loadNotify();
			}});
	}
	
	public void refreshUI() { 
		currentList.clear();
		Set<String> existing = new HashSet<String>();
		for( CommunityRecord rec : currentRecs ) { 
			
			if( existing.contains(rec.getUrl()) ) {
				System.out.println("skipping duplicate url: " + rec.getUrl());
				continue;
			}
			
			currentList.addItem(rec.getUrl());
			
			existing.add(rec.getUrl());
		}
	}
	
	public class AddServerDialog extends OneSwarmDialogBox {
		public AddServerDialog() {
			VerticalPanel vp = new VerticalPanel();

			final CommunityServerAddPanel p = new CommunityServerAddPanel(CommunityServerAddPanel.DEFAULT_COMMUNITY_SERVER, "", "", 
					msg.settings_net_community_contact_default_group(), false, false, false, false, 
					CommunityServerAddPanel.DEFAULT_PRUNING_THRESHOLD, null, null, false, true, true, 0, false);
			vp.add(p);
			this.setWidget(vp);
			this.setText(msg.settings_net_community_server_add());
			
			HorizontalPanel rhs = new HorizontalPanel();
			Button saveButton = new Button(msg.button_add());
			Button cancelButton = new Button(msg.button_cancel());
			rhs.add(cancelButton);
			rhs.add(saveButton);
			rhs.setSpacing(3);
//			rhs.setWidth("100%");
			com.google.gwt.user.client.ui.Widget hrule = new SimplePanel();
			hrule.addStyleName(SettingsDialog.CSS_HRULE);
			vp.add(hrule);
			vp.add(rhs);
			vp.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);
			
			cancelButton.addClickHandler(new ClickHandler(){
				public void onClick(ClickEvent event) {
					hide();
				}});
			
			saveButton.addClickHandler(new ClickHandler(){
				public void onClick(ClickEvent event) {
					
					if( p.getURL().length() == 0 ) {
						Window.alert(msg.settings_net_community_server_url_prompt());
						return;
					}
					
					currentList.addItem(p.getURL());
					currentRecs.add(new CommunityRecord(p));
					hide();
				}});
		}
	}
	
	public class EditServerDialog extends OneSwarmDialogBox {
		
		CommunityRecord mRec;
		
		public EditServerDialog(CommunityRecord rec) {
			mRec = rec;
			VerticalPanel vp = new VerticalPanel();

			final CommunityServerAddPanel p = new CommunityServerAddPanel(rec);
			vp.add(p);
			this.setWidget(vp);
			this.setText(msg.settings_net_community_server_add());
			
			HorizontalPanel rhs = new HorizontalPanel();
			Button saveButton = new Button(msg.button_save());
			Button cancelButton = new Button(msg.button_cancel());
			rhs.add(cancelButton);
			rhs.add(saveButton);
			rhs.setSpacing(3);

			com.google.gwt.user.client.ui.Widget hrule = new SimplePanel();
			hrule.addStyleName(SettingsDialog.CSS_HRULE);
			vp.add(hrule);
			vp.add(rhs);
			vp.setCellHorizontalAlignment(rhs, HorizontalPanel.ALIGN_RIGHT);
			
			cancelButton.addClickHandler(new ClickHandler(){
				public void onClick(ClickEvent event) {
					hide();
				}});
			
			saveButton.addClickHandler(new ClickHandler(){
				public void onClick(ClickEvent event) {
					
					if( p.getURL().length() == 0 ) {
						Window.alert(msg.settings_net_community_server_url_prompt());
						return;
					}
					
					int insertAt = currentRecs.indexOf(mRec);
					currentRecs.remove(insertAt);
					currentRecs.add(insertAt, new CommunityRecord(p));
					hide();
				}});
		}
	}

	public void onClick(ClickEvent event) {
		if( event.getSource().equals(addButton) ) {
			AddServerDialog dlg = new AddServerDialog(); 
			dlg.show();
			dlg.setVisible(false);
			dlg.center();
			dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
			dlg.setVisible(true);
		} else if( event.getSource().equals(removeButton) ) {
			int index = currentList.getSelectedIndex();
			if( index != -1 ) {
				currentList.removeItem(index);
				currentRecs.remove(index);
			}
		} else if( event.getSource().equals(propertiesButton) ) {
			int index = currentList.getSelectedIndex();
			if( index != -1 ) {
				EditServerDialog dlg = new EditServerDialog(currentRecs.get(index)); 
				dlg.show();
				dlg.setVisible(false);
				dlg.center();
				dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
				dlg.setVisible(true);
			}
		}
	}

	public void sync() {
		ArrayList<String> params = new ArrayList<String>();
		for( CommunityRecord rec : currentRecs ) {
			for( String t : rec.toTokens() ) {
				params.add(t);
			}
		}
		
		OneSwarmRPCClient.getService().setStringListParameterValue(OneSwarmRPCClient.getSessionID(), "oneswarm.community.servers", params, new AsyncCallback<Void>(){
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Void result) {
				System.out.println("saved community servers successfully");
			}});
	}
	
	String validData() {
		return null;
	}
}
