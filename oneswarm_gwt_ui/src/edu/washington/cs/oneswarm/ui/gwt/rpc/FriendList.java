package edu.washington.cs.oneswarm.ui.gwt.rpc;

import java.util.Arrays;

import com.google.gwt.user.client.rpc.IsSerializable;

public class FriendList implements IsSerializable {

    private FriendInfoLite[] friendList;
    private int listId;

    /*
     * null constructor + getters and setters are needed for gwt isSerializible
     */
    public FriendList() {
    }

    public FriendList(FriendInfoLite[] friendList) {
        this.friendList = friendList;

        int[] ids = new int[friendList.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = friendList[i].getHashOfStateFields();
        }
        this.listId = Arrays.hashCode(ids);
    }

    public FriendList(int listId) {
        this.listId = listId;
        this.friendList = new FriendInfoLite[0];
    }

    public FriendInfoLite[] getFriendList() {
        return friendList;
    }

    public int getListId() {
        return listId;
    }

    public void setFriendList(FriendInfoLite[] friendList) {
        this.friendList = friendList;
    }

    public void setListId(int listId) {
        this.listId = listId;
    }
}
