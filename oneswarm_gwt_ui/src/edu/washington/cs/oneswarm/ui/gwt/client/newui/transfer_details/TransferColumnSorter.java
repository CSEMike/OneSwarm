package edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.gen2.table.client.SortableGrid;
import com.google.gwt.gen2.table.client.SortableGrid.ColumnSorter;
import com.google.gwt.gen2.table.client.SortableGrid.ColumnSorterCallback;
import com.google.gwt.gen2.table.client.TableModelHelper.ColumnSortList;
import com.google.gwt.user.client.ui.Widget;

public class TransferColumnSorter extends ColumnSorter {
	@Override
	public void onSortColumn(SortableGrid grid, ColumnSortList sortList, ColumnSorterCallback callback) {
		// Get the primary column and sort order
		int column = sortList.getPrimaryColumn();
		boolean ascending = sortList.isPrimaryAscending();

		Integer[] perm = new Integer[grid.getRowCount()];
		for (int i = 0; i < perm.length; i++)
			perm[i] = new Integer(i);

		final List<Widget> entries = new ArrayList<Widget>(grid.getRowCount());
		boolean comparable = false;
		for (int rItr = 0; rItr < grid.getRowCount(); rItr++) {
			Widget w = grid.getWidget(rItr, column);
			entries.add(w);
			if (w instanceof Comparable)
				comparable = true;
		}

		if (comparable) {
			Arrays.sort(perm, new Comparator<Integer>() {
				public int compare(Integer o1, Integer o2) {
					/**
					 * Typecheck so we can use labels to indicate completion instead of
					 * 100% progress bars. 
					 */
					if (!(entries.get(o1) instanceof Comparable))
						return -1;
					else if (!(entries.get(o2) instanceof Comparable))
						return 1;

					return ((Comparable) entries.get(o1)).compareTo(entries.get(o2));
				}
			});
		} else {
			Arrays.sort(perm, new Comparator<Integer>() {
				public int compare(Integer o1, Integer o2) {
					return entries.get(o1).getElement().getInnerText().compareTo(entries.get(o2).getElement().getInnerText());
				}
			});
		}

		int[] intperm = new int[perm.length];
		for (int i = 0; i < perm.length; i++) {
			//  System.out.println(i + " -> " + perm[i]);
			if (ascending)
				intperm[i] = perm[i];
			else
				intperm[intperm.length - i - 1] = perm[i];
		}

		// Use the callback to complete the sorting
		callback.onSortingComplete(intperm);
	}
}
