package edu.washington.cs.oneswarm.ui.gwt.client.newui.tableutils;

import com.google.gwt.user.client.ui.Label;

public class SortableInt extends Label implements Comparable {

	int val = 0;

	public SortableInt(int val) {
		super(Integer.toString(val));
		this.val = val;
	}

	public int compareTo(Object o) {
		if (o instanceof SortableInt) {
			return val - ((SortableInt)o).val;
		}
		return -1;
	}
}