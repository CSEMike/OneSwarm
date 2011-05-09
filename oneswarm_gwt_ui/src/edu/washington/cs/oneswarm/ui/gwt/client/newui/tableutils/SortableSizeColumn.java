package edu.washington.cs.oneswarm.ui.gwt.client.newui.tableutils;

import com.google.gwt.user.client.ui.Label;

import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class SortableSizeColumn extends Label implements Comparable {

    long mSize = 0;

    public SortableSizeColumn(long inSizeBytes) {
        super(StringTools.formatRate(inSizeBytes));
        mSize = inSizeBytes;
    }

    public int compareTo(Object o) {
        if (o instanceof SortableSizeColumn) {
            long diff = this.mSize - ((SortableSizeColumn) o).mSize;
            if (diff > 0)
                return 1;
            else if (diff < 0)
                return -1;

            return 0;
        }
        return -1;
    }
}