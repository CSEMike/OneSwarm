package edu.washington.cs.oneswarm.f2f.permissions;

import java.util.ArrayList;
import java.util.List;

public class GroupBean 
{
	transient public static final GroupBean PUBLIC;
	transient public static final GroupBean ALL_FRIENDS;
	
	static {
		List<String> scratch = new ArrayList<String>();
		ALL_FRIENDS = new GroupBean();
		ALL_FRIENDS.setGroupName("All friends");
		ALL_FRIENDS.setGroupID(1);
		scratch.add("friends_only");
		ALL_FRIENDS.setMemberKeys(scratch);
		scratch = new ArrayList<String>();
		PUBLIC = new GroupBean();
		PUBLIC.setGroupName("Public Internet");
		PUBLIC.setGroupID(2);
		scratch.add("public_internet");
		PUBLIC.setMemberKeys(scratch);
	}
	
	public static GroupBean createGroup( String name, List<String> keys, boolean isUserGroup, long id ) { 
		GroupBean out = new GroupBean();
		
		out.setGroupName(name);
		out.setMemberKeys(keys);
		out.setUserGroup(isUserGroup);
		out.setGroupID(id);
		
		return out;
	}
	
	String groupName;
	List<String> memberKeys;
	boolean isUserGroup;
	long groupID;
	
	public GroupBean() {}
	
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public List<String> getMemberKeys() {
		return memberKeys;
	}
	public void setMemberKeys(List<String> memberKeys) {
		this.memberKeys = memberKeys;
	}
	
	public int hashCode() 
	{
		return groupName.hashCode();
	}
	
	public boolean equals( Object rhs )
	{
		if( !(rhs instanceof GroupBean) )
		{
			return false;
		}
		return ((GroupBean)rhs).groupName.equals(this.groupName);
	}
	public boolean isUserGroup() {
		return isUserGroup;
	}
	public void setUserGroup(boolean isUserGroup) {
		this.isUserGroup = isUserGroup;
	}
	public long getGroupID() {
		return groupID;
	}
	public void setGroupID(long groupID) {
		this.groupID = groupID;
	}
	
	public String toString() {
		return getGroupName() + " (" + getGroupID() + ")";
	}
}

