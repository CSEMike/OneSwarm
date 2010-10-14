package edu.washington.cs.oneswarm.ui.gwt.client.newui.tableutils;

import com.google.gwt.user.client.ui.Label;

public class SortableDouble extends Label implements Comparable {

	Double val;

	public SortableDouble(double val) {
		super(Double.toString(val));
		this.val = val;
	}

	public int compareTo(Object o) {
		if (o instanceof SortableDouble) {
			return val.compareTo( ((SortableDouble)o).val );
		}
		return -1;
	}
}