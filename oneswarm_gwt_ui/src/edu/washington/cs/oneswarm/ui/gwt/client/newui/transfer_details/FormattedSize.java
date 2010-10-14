package edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details;

import com.google.gwt.user.client.ui.Label;

import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class FormattedSize extends Label implements Comparable {
	long mBytes;
	String mSuffix;
	
	public FormattedSize(long inBytes)
	{
		this(inBytes, "");
	}

	public FormattedSize(long inBytes, String inSuffix) {
		super();

		mBytes = inBytes;
		mSuffix = inSuffix;

		setText(StringTools.formatRate(inBytes) + mSuffix);
	}
	
	public void update( long inBytes )
	{
		mBytes = inBytes;
		setText(StringTools.formatRate(inBytes) + mSuffix);
	}
	
	public long getBytes() { return mBytes; }

	public int compareTo(Object o) {
		if (o instanceof FormattedSize) {
			return (int) (mBytes - ((FormattedSize) o).mBytes);
		} else {
			return -1;
		}
	}
}