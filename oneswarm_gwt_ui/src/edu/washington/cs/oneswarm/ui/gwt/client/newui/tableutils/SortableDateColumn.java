package edu.washington.cs.oneswarm.ui.gwt.client.newui.tableutils;

import java.util.Date;

import com.google.gwt.user.client.ui.Label;

import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;

public class SortableDateColumn extends Label implements Comparable {

    public enum FormatStyle {
        APPLE, NOTIME, FULL;
    }

    Date mDate = null;

    public SortableDateColumn(Date inDate, FormatStyle format) {
        super();
        if (inDate == null) {
            mDate = new Date(0);
            setText("Unknown");
        } else {
            mDate = inDate;
            if (inDate.equals(new Date(0))) {
                setText("Unknown");
            } else {
                switch (format) {
                case APPLE:
                    setText(StringTools.formatDateAppleLike(inDate, false));
                    break;
                case NOTIME:
                    setText(StringTools.formatDateMonthDayYear(inDate));
                    break;
                case FULL:
                    setText(inDate.toString());
                    break;
                }
            }
        }
    }

    public int compareTo(Object o) {
        if (o instanceof SortableDateColumn) {
            return this.mDate.compareTo(((SortableDateColumn) o).mDate);
        }
        return -1;
    }
}
