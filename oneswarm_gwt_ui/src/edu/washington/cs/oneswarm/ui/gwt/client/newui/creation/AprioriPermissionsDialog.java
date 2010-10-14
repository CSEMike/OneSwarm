package edu.washington.cs.oneswarm.ui.gwt.client.newui.creation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.GroupsListSorter;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.MembershipList;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.MembershipListListener;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;

public class AprioriPermissionsDialog extends OneSwarmDialogBox
{
	public static final int WIDTH = 600;
	public static final int HEIGHT = 415;
	
	VerticalPanel mainPanel = new VerticalPanel();
	
	boolean all_friends_added = false;
	private Button cancelButton = new Button("Cancel");
	private Button okButton = new Button("Save");
	
	List<PermissionsGroup> initial_groups = null;
	
	public AprioriPermissionsDialog( String inName, String inHeader, List<PermissionsGroup> initialGroups, final ApriorPermissionsCallback inCallback )
	{
		super(false, true, false);
		
		final boolean inF2F = initialGroups.contains(new PermissionsGroup(PermissionsGroup.ALL_FRIENDS));
		final boolean inPublic = initialGroups.contains(new PermissionsGroup(PermissionsGroup.PUBLIC_INTERNET));
		
		this.setText("Visibility: " + inName);
		
		initial_groups = initialGroups;
		
		Label headerLabel = new Label(inHeader);
		headerLabel.addStyleName(CSS_DIALOG_HEADER);
		headerLabel.setWidth(WIDTH + "px");
		mainPanel.add(headerLabel);
		
		mainPanel.add(new Label("Loading..."));
		
		OneSwarmRPCClient.getService().getAllGroups(OneSwarmRPCClient.getSessionID(), new AsyncCallback<ArrayList<PermissionsGroup>>(){
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
				hide();
			}

			public void onSuccess(ArrayList<PermissionsGroup> result) {
				initUI(result);
			}
		});
		
		okButton.addClickListener(new ClickListener(){
			public void onClick(Widget sender) {
				okButton.setEnabled(false);
				
				inCallback.permissionsDefined(sharing_with_groups.getMembers());
				hide();
			}});
		cancelButton.addClickListener(new ClickListener(){
			public void onClick(Widget sender) {
				hide();
				inCallback.cancelled();
			}});
		
		mainPanel.setHeight(HEIGHT+"px");
		this.setWidget(mainPanel);
	}
	
	MembershipList<PermissionsGroup> sharing_with_groups = null;
	
	private void initUI( final ArrayList<PermissionsGroup> all_groups )
	{
		mainPanel.remove(1);
		
		
		VerticalPanel bottomPane = new VerticalPanel();
		
		bottomPane.setWidth(WIDTH+"px");
		
		ArrayList<PermissionsGroup> swarm_groups = new ArrayList<PermissionsGroup>();
		for( PermissionsGroup g : initial_groups )
		{
			swarm_groups.add(g);
		}
		
		ArrayList<PermissionsGroup> all_sub_shared = new ArrayList<PermissionsGroup>();
		for( PermissionsGroup g : all_groups )
		{
			if( !swarm_groups.contains(g) )
			{
				all_sub_shared.add(g);
			}
		}
		
		final MembershipList<PermissionsGroup> available_groups = new MembershipList<PermissionsGroup>("Available groups:", true, all_groups, swarm_groups, true, new GroupsListSorter());
		sharing_with_groups = new MembershipList<PermissionsGroup>("Sharing with:", false, all_groups, all_sub_shared, true, new GroupsListSorter());
		
		final PermissionsGroup public_net = new PermissionsGroup(PermissionsGroup.PUBLIC_INTERNET);
		final PermissionsGroup all_friends = new PermissionsGroup(PermissionsGroup.ALL_FRIENDS);
		Set<PermissionsGroup> hs = new HashSet<PermissionsGroup>();
		hs.add(public_net);
		
		all_friends_added = false;
		for( PermissionsGroup g : swarm_groups )
		{
			if( g.isAllFriends() )
			{
				all_friends_added = true;
			}
		}
		
		available_groups.addListener(new MembershipListListener<PermissionsGroup>(){
			public void objectEvent(MembershipList<PermissionsGroup> list, PermissionsGroup inObject) {
				System.out.println("groups remove event: " + inObject);
				sharing_with_groups.restoreExcluded(inObject);
				
				/**
				 * Here we are removing some group from available groups and adding to sharing with. 
				 * If we're adding something that's NOT the 'public' group and we're in 'share with all friends' mode, 
				 * we need to remove all the more specific groups. 
				 */
				if( all_friends_added && inObject.isPublicInternet() == false )
				{
					System.out.println("adding specific group when all friends is present");
					all_friends_added = false;
					sharing_with_groups.addExcluded(all_friends);
					available_groups.restoreExcluded(all_friends);
				}
				/**
				 * Here we are adding the 'all friends' object, so we need to remove all the previous, 
				 * more specific groups. 
				 */
				else if( inObject.isAllFriends() )
				{
					all_friends_added = true;
					// need to remove anything more specific than this. 
					for( PermissionsGroup g : all_groups )
					{
						if( g.isPublicInternet() == false && g.isAllFriends() == false )
						{
							sharing_with_groups.addExcluded(g);
							available_groups.restoreExcluded(g);
						}
					}
				}
			}});
		
		
		sharing_with_groups.addListener(new MembershipListListener<PermissionsGroup>(){
			public void objectEvent(MembershipList<PermissionsGroup> list, PermissionsGroup inObject) {
				System.out.println("permitted remove event: " + inObject);
				available_groups.restoreExcluded(inObject);
			}});
		
		HorizontalPanel hp = new HorizontalPanel();
		hp.setWidth("100%");
		
		hp.add(sharing_with_groups);
		available_groups.setWidth("100%");
		hp.add(available_groups);
		hp.setSpacing(3);
		hp.setCellWidth(available_groups, "49%");
		hp.setCellWidth(sharing_with_groups, "49%");
		
		hp.setCellHorizontalAlignment(sharing_with_groups, HorizontalPanel.ALIGN_LEFT);
		hp.setCellHorizontalAlignment(available_groups, HorizontalPanel.ALIGN_RIGHT);
		
		bottomPane.add(hp);
		
		mainPanel.add(bottomPane);
		
		HorizontalPanel buttons_hp = new HorizontalPanel();
		buttons_hp.add(cancelButton);
		buttons_hp.add(okButton);
		buttons_hp.setSpacing(3);
		mainPanel.add(buttons_hp);
		mainPanel.setCellHorizontalAlignment(buttons_hp, HorizontalPanel.ALIGN_RIGHT);
	}
}
