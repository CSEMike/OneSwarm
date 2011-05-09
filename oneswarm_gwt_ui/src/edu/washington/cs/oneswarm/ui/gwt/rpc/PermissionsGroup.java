package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

public class PermissionsGroup implements IsSerializable {

    /**
     * These need to be kept in sync with the GroupBean/SwarmBean classes in the
     * core
     */
    public static final String ALL_FRIENDS = "friends_only";
    public static final String PUBLIC_INTERNET = "public_internet";

    public PermissionsGroup() {
    }

    private LinkedList<String> mMembershipKeys = new LinkedList<String>();
    private boolean mUserGroup;
    private String mGroupName;
    private long mID;

    public List<String> getKeys() {
        return (List<String>) mMembershipKeys;
    }

    public long getGroupID() {
        return mID;
    }

    public void setGroupID(long inID) {
        mID = inID;
    }

    public PermissionsGroup(String special_type) {
        if (special_type.equals(ALL_FRIENDS)) {
            mGroupName = "All friends";
            setGroupID(1);
        } else if (special_type.equals(PUBLIC_INTERNET)) {
            mGroupName = "Public Internet";
            setGroupID(2);
        }

        mMembershipKeys = new LinkedList<String>();
        mMembershipKeys.add(special_type);
    }

    public PermissionsGroup(String inGroupName, String[] inFriendKeys, boolean inUserGroup,
            long inID) {
        mMembershipKeys = new LinkedList<String>();
        for (String s : inFriendKeys)
            mMembershipKeys.add(s);
        mUserGroup = inUserGroup;
        mGroupName = inGroupName;
        setGroupID(inID);
    }

    public boolean isSpecial() {
        return isUserGroup() || isAllFriends() || isPublicInternet();
    }

    public String getName() {
        return mGroupName;
    }

    public boolean isAllFriends() {
        if (mMembershipKeys.size() == 1) {
            return mMembershipKeys.get(0).equals(ALL_FRIENDS);
        }
        return false;
    }

    public boolean isPublicInternet() {
        if (mMembershipKeys.size() == 1) {
            return mMembershipKeys.get(0).equals(PUBLIC_INTERNET);
        }
        return false;
    }

    public boolean isUserGroup() {
        return mUserGroup;
    }

    public void addUser(String inKey) {
        if (isSpecial()) {
            System.err.println("trying to add a key to special user: " + inKey);
        } else {
            mMembershipKeys.add(inKey);
        }
    }

    public void removeUser(String inKey) {
        if (isSpecial()) {
            System.err.println("trying to remove a key to special user: " + inKey + " from "
                    + mGroupName);
        } else {
            if (mMembershipKeys.remove(inKey) == false) {
                System.err.println("tried to remove nonexistent key from group: " + inKey
                        + " from " + mGroupName);
            }
        }
    }

    public boolean containsUser(String inKey) {
        return mMembershipKeys.contains(inKey);
    }

    public String toString() {
        return mGroupName;
    }

    public int hashCode() {
        return mGroupName.hashCode();
    }

    public boolean equals(Object inrhs) {
        if (!(inrhs instanceof PermissionsGroup)) {
            return false;
        }

        PermissionsGroup rhs = (PermissionsGroup) inrhs;

        if (mMembershipKeys.size() != rhs.mMembershipKeys.size()) {
            return false;
        }

        if (isUserGroup() != rhs.isUserGroup()) {
            return false;
        }

        for (int i = 0; i < mMembershipKeys.size(); i++) {
            if (mMembershipKeys.contains(rhs.mMembershipKeys.get(i)) == false) {
                return false;
            }
        }
        return true;
    }
}
