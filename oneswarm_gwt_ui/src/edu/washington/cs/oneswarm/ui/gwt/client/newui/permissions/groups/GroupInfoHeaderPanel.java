package edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions.groups;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class GroupInfoHeaderPanel extends HorizontalPanel {

	public static final String CSS_TORRENT_HEADER_NAME = "os-torrent_header_name";
	public static final String CSS_TORRENT_HEADER_SUB = "os-torrent_header_sub";
	
	Label friendsCountLabel = new Label("");
	
	public GroupInfoHeaderPanel(PermissionsGroup inGroup) {
		
//		Image img = new Image("group.png");
//		img.setWidth("64px");
//		img.setHeight("64px");
//		this.add(img);
//		this.setCellWidth(img, "64px");
//		this.setCellHorizontalAlignment(img, ALIGN_LEFT);
		SimplePanel space = new SimplePanel();
		this.add(space);
		this.setCellHeight(space, "64px");
		this.setSpacing(3);
		VerticalPanel vp = new VerticalPanel();
		Label l = new Label(StringTools.truncate(inGroup.getName(), 58, true));
		l.setTitle(inGroup.getName());
		l.addStyleName(CSS_TORRENT_HEADER_NAME);
		vp.add(l);
		String word = "friends";
		if( inGroup.getKeys().size() == 1 )
		{
			word = "friend";
		}
		friendsCountLabel.setText(inGroup.getKeys().size() + " " + word);
		friendsCountLabel.addStyleName(CSS_TORRENT_HEADER_SUB);
		vp.add(friendsCountLabel);
		l = new Label(" ");
		l.addStyleName(CSS_TORRENT_HEADER_SUB);
		vp.add(l);
		this.add(vp);
		setCellHorizontalAlignment(vp, ALIGN_LEFT);
	}
	
	public void updateFriendCount( int count ) {
		String word = "friends";
		if( count == 1 )
		{
			word = "friend";
		}
		friendsCountLabel.setText(count + " " + word);
	}
	
}
