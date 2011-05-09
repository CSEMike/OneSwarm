package edu.washington.cs.oneswarm.ui.gwt.client.newui.permissions;

public interface MembershipListListener<T> {
    public void objectEvent(MembershipList<T> list, T inObject);
}
