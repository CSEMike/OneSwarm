package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ReportableException implements IsSerializable {
    String mText;

    public ReportableException() {
    }

    public ReportableException(String inText) {
        mText = inText;
    }

    public String getText() {
        return mText;
    }

    public String toString() {
        return getText();
    }
}
