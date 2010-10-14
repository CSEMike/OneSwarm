package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.Strings;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.SwarmsBrowser;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class FriendsDetailsListPanel extends VerticalPanel implements Updateable {
	
//	public static final String SHOW_BLOCK_FRIENDS_COOKIE = "ShowBlockedFriends";
	
	VerticalPanel filesFlowPanel = this;
	private FriendsDetailsTable friendstable = null;
	private long mFriendsChangesCheck = 0;
	
	private FriendInfo datecalculationhelper = new FriendInfo(null);
	Date lastDate = null;
	
	private Label lastUpdatedTable = new Label();
	
	private List<Button> buttonsNeedingSelectedItem = new ArrayList<Button>();
	private Label loadingLabel;
	
	private CheckBox showBlockedCheckBox = new CheckBox("Show blocked friends");
	private SwarmsBrowser mBrowser;
	
	public FriendsDetailsListPanel(SwarmsBrowser inBrowser) {
		
		mBrowser = inBrowser;
		
		GWT.runAsync(new RunAsyncCallback() {
			public void onFailure(Throwable reason) {
			}

			public void onSuccess() {
				// preloading
				if( mBrowser == null ) {
					return;
				}
				
				onInitialized();
			}			
		});
	}
	
	protected FriendsDetailsListPanel onInitialized() {
		setWidth("100%");
		
		//DOM.setStyleAttribute(this.getElement(), "height", "200px");
		loadingLabel = new Label("Loading...");
		this.add(loadingLabel);
		//this.setCellHorizontalAlignment(loadingLabel, ALIGN_CENTER);
		
		/**
		 * All the RPC logic should probably be pulled out to this class since the overall view depends on 
		 * whether or not the RPC return any friends at all. 
		 */
		friendstable = new FriendsDetailsTable(mBrowser, new FriendsDetailsTable.RefreshCallback(){
			boolean mFirstRun = true; 
			public void refreshed(FriendList result) {
				if( mFirstRun ) {
					FriendsDetailsListPanel.this.remove(loadingLabel);
					if( result.getFriendList().length > 0 ) {
						initUI();
					} else {
						HTML msg = new HTML("<div id=\"" + OneSwarmCss.CSS_NOTHING_SHOWING + "\">" + Strings.get(Strings.NO_FRIENDS_MSG) + "</div>");
						add(msg);
					}
					mFirstRun = false;
				}
			}});
		
		return this;
	}
	
	public void initUI() {
		
		lastDate = new Date();
		lastUpdatedTable.setText("Last updated: < 1 minute");
		final Timer incrementlastUpdated = (new Timer() {
			public void run() {
				if( lastDate.getTime() + 60 * 1000 > System.currentTimeMillis() ) {
					lastUpdatedTable.setText("Last updated: < 1 minute");
				}
				else {
					lastUpdatedTable.setText("Last updated: " + StringTools.formatDateAppleLike(lastDate));
				}
			}
		});
		incrementlastUpdated.scheduleRepeating(5000);
		final Button refresh = new Button("Refresh");
		final Button selectall = new Button("Select all");
		final Button deselectall = new Button("Deselect all");
		final Button selectNeverConnected = new Button("Select never connected");
		final Button forceConnect = new Button("Force Connect");
		final Button remove = new Button("Delete");
		final Button block = new Button("Swap blocked");
		final Button addfriend = new Button("Add Friend");
		final Button refresh2 = new Button("Refresh");
		final Button selectall2 = new Button("Select all");
		final Button selectNeverConnected2 = new Button("Select never connected");
		final Button deselectall2 = new Button("Deselect all");
		final Button forceConnect2 = new Button("Force Connect");
		final Button remove2 = new Button("Delete");
		final Button block2 = new Button("Swap blocked");
		final Button addfriend2 = new Button("Add Friend");
//		final Button undelete = new Button("Undelete");
//		final Button undelete2 = new Button("Undelete");
		final Button swaplimited = new Button("Change Limited");
		final Button swaplimited2 = new Button("Change Limited");
		final Button swapchat = new Button("Chat Allowed");
		final Button swapchat2 = new Button("Chat Allowed");
		
		/**
		 * Some code to make sure that buttons in this panel reflect the selection state in the actual table 
		 */
		buttonsNeedingSelectedItem.add(forceConnect);
		buttonsNeedingSelectedItem.add(forceConnect2);
		buttonsNeedingSelectedItem.add(remove);
		buttonsNeedingSelectedItem.add(remove2);
		buttonsNeedingSelectedItem.add(block);
		buttonsNeedingSelectedItem.add(block2);
		buttonsNeedingSelectedItem.add(swaplimited);
		buttonsNeedingSelectedItem.add(swaplimited2);
		buttonsNeedingSelectedItem.add(swapchat);
		buttonsNeedingSelectedItem.add(swapchat2);
		// initially, nothing is selected
		for( Button b : buttonsNeedingSelectedItem ) {
			b.setEnabled(false);
		}
		
		remove2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		selectall2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		deselectall2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		selectNeverConnected.addStyleName(OneSwarmCss.SMALL_BUTTON);
		selectNeverConnected2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		forceConnect2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		addfriend2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		refresh2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		remove.addStyleName(OneSwarmCss.SMALL_BUTTON);
		block.addStyleName(OneSwarmCss.SMALL_BUTTON);
		block2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		selectall.addStyleName(OneSwarmCss.SMALL_BUTTON);
		deselectall.addStyleName(OneSwarmCss.SMALL_BUTTON);
		forceConnect.addStyleName(OneSwarmCss.SMALL_BUTTON);
		addfriend.addStyleName(OneSwarmCss.SMALL_BUTTON);
		refresh.addStyleName(OneSwarmCss.SMALL_BUTTON);
//		undelete.addStyleName(SaveLocationPanel.CSS_SMALL_BUTTON);
//		undelete2.addStyleName(SaveLocationPanel.CSS_SMALL_BUTTON);
		swaplimited.addStyleName(OneSwarmCss.SMALL_BUTTON);
		swaplimited2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		swapchat.addStyleName(OneSwarmCss.SMALL_BUTTON);
		swapchat2.addStyleName(OneSwarmCss.SMALL_BUTTON);
		HorizontalPanel topcontrols = new HorizontalPanel();
		HorizontalPanel top = new HorizontalPanel();
		topcontrols.add(selectall2);
		topcontrols.add(deselectall2);
		topcontrols.add(selectNeverConnected2);
		topcontrols.add(forceConnect2);
		topcontrols.add(swaplimited2);
		topcontrols.add(swapchat2);
		topcontrols.add(remove2);
		topcontrols.add(block2);
//		topcontrols.add(undelete2);
		topcontrols.add(addfriend2);
		top.add(lastUpdatedTable);
		top.setCellVerticalAlignment(lastUpdatedTable, VerticalPanel.ALIGN_MIDDLE);
		top.setSpacing(3);
		top.add(refresh);
		top.add(showBlockedCheckBox);
		filesFlowPanel.add(top);
		filesFlowPanel.add(new Label(" "));
		filesFlowPanel.add(topcontrols);
		
		showBlockedCheckBox.addClickHandler(new ClickHandler(){
			long lastClick = 0; // work-around for GWT bug wherein clicking on label generates two back-to-back click events
			public void onClick(ClickEvent event) {
				if( (System.currentTimeMillis() - lastClick) < 100 ) {
					return;
				}
				lastClick = System.currentTimeMillis();
				friendstable.setShowBlocked(showBlockedCheckBox.getValue());
				System.out.println("showing blocked: " + showBlockedCheckBox.getValue());
				friendstable.refresh();
			}});
		
		showBlockedCheckBox.setValue(false);
		
		friendstable.addSelectionCallback(new FriendsDetailsTable.SelectionCallback(){
			public void deselectedAll() {
				for( Button b : buttonsNeedingSelectedItem ) {
					b.setEnabled(false);
				}
			}

			public void somethingSelected() {
				for( Button b : buttonsNeedingSelectedItem ) {
					b.setEnabled(true);
				}
			}});
		filesFlowPanel.add(friendstable);
		
		HorizontalPanel bottomcontrols = new HorizontalPanel();
		bottomcontrols.add(selectall);
		bottomcontrols.add(deselectall);
		bottomcontrols.add(selectNeverConnected);
		bottomcontrols.add(forceConnect);
		bottomcontrols.add(swaplimited);
		bottomcontrols.add(swapchat);
		bottomcontrols.add(remove);
		bottomcontrols.add(block);
//		topcontrols.add(undelete);
		bottomcontrols.add(addfriend);
		filesFlowPanel.add(bottomcontrols);
//		filesFlowPanel.add(selectall);
//		filesFlowPanel.add(deselectall);
//		filesFlowPanel.add(forceConnect);
//		filesFlowPanel.add(swaplimited);
//		filesFlowPanel.add(swapchat);
//		filesFlowPanel.add(remove);
//		filesFlowPanel.add(undelete);
//		filesFlowPanel.add(addfriend);
		ClickListener clk = new ClickListener() {
			public void onClick(Widget w) {
				if (w.equals(selectall) || w.equals(selectall2)) {
					friendstable.selectall();
				} else if (w.equals(deselectall) || w.equals(deselectall2)){
					friendstable.deselectall();
				} else if (w.equals(forceConnect) || w.equals(forceConnect2)) {
					friendstable.forceConnect();
				} else if (w.equals(remove) || w.equals(remove2)) {
					lastDate = new Date();
					lastUpdatedTable.setText("Last updated: " + StringTools.formatDateAppleLike(lastDate));
					friendstable.removeClicked();
				} else if( w.equals(block) || w.equals(block2)) {
					lastUpdatedTable.setText("Last updated: " + StringTools.formatDateAppleLike(lastDate));
					friendstable.blockClicked();
				} else if (w.equals(addfriend) || w.equals(addfriend2)) {
					friendstable.addFriends();
				} else if (w.equals(refresh)){
					lastDate = new Date();
					lastUpdatedTable.setText("Last updated: " + StringTools.formatDateAppleLike(lastDate));
					friendstable.refresh();
//				} else if (w.equals(undelete) || w.equals(undelete2)){
//					lastDate = new Date();
//					lastUpdatedTable.setText("Last updated: " + datecalculationhelper.appleLikeDate(lastDate));
//					friendstable.undelete();
				} else if (w.equals(swapchat) || w.equals(swapchat2)) {
					lastDate = new Date();
					lastUpdatedTable.setText("Last updated: " + StringTools.formatDateAppleLike(lastDate));
					friendstable.swapchat();
				} else if (w.equals(swaplimited) || w.equals(swaplimited2)) {
					lastDate = new Date();
					lastUpdatedTable.setText("Last updated: " + StringTools.formatDateAppleLike(lastDate));
					friendstable.swaplimited();
				} else if( w.equals(selectNeverConnected) || w.equals(selectNeverConnected2) ) {
					friendstable.selectNeverConnected();
				} else {
				}
			}
		};
		selectall.addClickListener(clk);
		deselectall.addClickListener(clk);
		selectNeverConnected.addClickListener(clk);
		selectNeverConnected2.addClickListener(clk);
		forceConnect.addClickListener(clk);
		remove.addClickListener(clk);
		addfriend.addClickListener(clk);
		refresh.addClickListener(clk);
		selectall2.addClickListener(clk);
		deselectall2.addClickListener(clk);
		forceConnect2.addClickListener(clk);
		remove2.addClickListener(clk);
		addfriend2.addClickListener(clk);
//		undelete.addClickListener(clk);
//		undelete2.addClickListener(clk);
		swapchat.addClickListener(clk);
		swapchat2.addClickListener(clk);
		swaplimited.addClickListener(clk);
		swaplimited2.addClickListener(clk);
		block.addClickListener(clk);
		block2.addClickListener(clk);
	}
	
	protected void onAttach() {
		super.onAttach();
		OneSwarmGWT.addToUpdateTask(this);
	}
	
	protected void onDetach() {
		super.onDetach();
		OneSwarmGWT.removeFromUpdateTask(this);
	}
	
	public void update(int count) {
		/*Checks if any changes have been made outside the friendstable (if a change is made inside a friends-panel or a friend added/deleted)*/
		if ((mFriendsChangesCheck < System.currentTimeMillis()) ) {
			OneSwarmRPCClient.getService().recentFriendChanges(OneSwarmRPCClient.getSessionID(), new AsyncCallback<Boolean>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}
				
				public void onSuccess(Boolean result) {
					if (result) {
						friendstable.refresh();
						lastDate = new Date();
						lastUpdatedTable.setText("Last updated: " + StringTools.formatDateAppleLike(lastDate));
					}
				}
			});
			
			mFriendsChangesCheck = System.currentTimeMillis() + 1000;
		}
	}
}
