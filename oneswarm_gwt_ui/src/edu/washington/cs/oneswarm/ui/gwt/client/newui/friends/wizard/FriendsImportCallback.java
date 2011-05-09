/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;

public interface FriendsImportCallback {
    public void cancel();

    public void connectSuccesful(FriendInfoLite[] changes, boolean showSkip);

    public void back();
}