package edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions;

import java.util.Comparator;

import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;

public class GroupsListSorter implements Comparator<PermissionsGroup> {

	private boolean isAllOrPub( PermissionsGroup g ) {
		return g.isPublicInternet() || g.isAllFriends();
	}
	
	public int compare(PermissionsGroup o1, PermissionsGroup o2) {
		if( isAllOrPub(o1) && isAllOrPub(o2) == false ) {
			return -1;
		} else if( isAllOrPub(o2) && isAllOrPub(o1) == false ) {
			return 1;
		} 
		return o1.toString().compareTo(o2.toString());
	}
	
}
