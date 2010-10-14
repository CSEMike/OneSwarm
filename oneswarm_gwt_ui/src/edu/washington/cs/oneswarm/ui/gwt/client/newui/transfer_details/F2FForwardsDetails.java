package edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.Strings;

public class F2FForwardsDetails extends ScrollTable implements Updateable {
	FixedWidthGrid mData = null;
	FixedWidthFlexTable mHeader = null;
	private long nextTime;

	public F2FForwardsDetails() {
		super(new FixedWidthGrid(0, Strings.F2F_DETAILS_COLUMNS.length), new FixedWidthFlexTable());

		mData = getDataTable();
		mHeader = getHeaderTable();

		mData.setSelectionPolicy(SelectionPolicy.ONE_ROW);

		/**
		 * not only for webpage-style scrolling but also to get this widget to
		 * declare it's needed size
		 */
		setScrollPolicy(ScrollPolicy.DISABLED);

		setResizePolicy(ResizePolicy.FILL_WIDTH);
		// setResizePolicy(ResizePolicy.FLOW);
		this.setWidth("99%");

		for (int i = 0; i < Strings.F2F_DETAILS_COLUMNS.length; i++) {
			mHeader.setText(0, i, Strings.F2F_DETAILS_COLUMNS[i]);
		}

		mData.setColumnSorter(new TransferColumnSorter());

		mHeader.setWidth("100%");
		mData.setWidth("100%");

		nextTime = 0;

		// refreshRPC();
	}

	public void onDetach() {
		super.onDetach();
		OneSwarmGWT.removeFromUpdateTask(this);
	}

	public void onAttach() {
		super.onAttach();
		OneSwarmGWT.addToUpdateTask(this);
		// fix column widths
		// int width = this.getOffsetWidth();
		// double[] fracs = new double[] { 0.165, 0.165, 0.165, 0.165, 0.165,
		// 0.165 };
		// for (int fItr = 0; fItr < fracs.length; fItr++) {
		// mHeader.setColumnWidth(fItr, (int) (fracs[fItr] * (double) width));
		// mData.setColumnWidth(fItr, (int) (fracs[fItr] * (double) width));
		// }
	}

	private int CHANNEL_ID_COLUMN = 0;
	private int RATE_COLUMN = 0;
	private int TOTAL_COLUMN = 0;

	public void update(int count) {
		if ((count % 1) == 0 && isVisible()) {
			if (System.currentTimeMillis() > nextTime) {
				nextTime = Long.MAX_VALUE;

				OneSwarmRPCClient.getService().getFriendTransferStats(OneSwarmRPCClient.getSessionID(), new AsyncCallback<ArrayList<HashMap<String, String>>>() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}

					public void onSuccess(ArrayList<HashMap<String, String>> result) {
						nextTime = System.currentTimeMillis() + 1000;

						List<Integer> toRemove = new ArrayList<Integer>();

						// first try to update everything...
						Set<Integer> used = new HashSet<Integer>();
						String rowIdentifyer;
						// after
						// first 3
						// fields
						for (int rItr = 0; rItr < mData.getRowCount(); rItr++) {

							rowIdentifyer = (((Label) mData.getWidget(rItr, CHANNEL_ID_COLUMN)).getText());

							boolean updated = false;
							for (int candidate = 0; candidate < result.size(); candidate++) {
								if (used.contains(candidate)) {
									continue;
								}

								if (rowIdentifyer.equals(result.get(candidate).get("id"))) {
									used.add(candidate);

									long rate = Long.parseLong(result.get(candidate).get("rate"));
									long total = Long.parseLong(result.get(candidate).get("total"));

									((FormattedSize) mData.getWidget(rItr, RATE_COLUMN)).update(rate);
									((FormattedSize) mData.getWidget(rItr, TOTAL_COLUMN)).update(total);
									updated = true;
								}
							}
							if (!updated) {
								toRemove.add(rItr);
							}
						}

						/*
						 * then remove the old ones
						 */
						Collections.sort(toRemove);
						Collections.reverse(toRemove);
						for (int r : toRemove)
							mData.removeRow(r);

						// then add rows for anything new (if this happens, we
						// need to re-sort)
						int rowCount = mData.getRowCount();
						int new_rows = 0;
						for (int candidate = 0; candidate < result.size(); candidate++) {
							if (used.contains(candidate))
								continue;

							String content = result.get(candidate).get("content");
							if (content.length() > 40) {
								content = content.substring(0, 37) + "...";
							}
							int row = 0;
							CHANNEL_ID_COLUMN = row;
							if (mData.getRowCount() <= new_rows + rowCount) {
								mData.resizeRows(new_rows + rowCount + 1);
							}
							mData.setWidget(new_rows + rowCount, row, new Label(result.get(candidate).get("id")));
							row++;
							mData.setWidget(new_rows + rowCount, row, new Label(content));
							row++;
							mData.setWidget(new_rows + rowCount, row, new Label(result.get(candidate).get("from")));
							row++;
							mData.setWidget(new_rows + rowCount, row, new Label(result.get(candidate).get("to")));
							row++;
							RATE_COLUMN = row;
							mData.setWidget(new_rows + rowCount, RATE_COLUMN, new FormattedSize(Long.parseLong(result.get(candidate).get("rate")), "ps"));
							row++;
							TOTAL_COLUMN = row;
							mData.setWidget(new_rows + rowCount, TOTAL_COLUMN, new FormattedSize(Long.parseLong(result.get(candidate).get("total"))));

							new_rows++;
						}

						if (used.size() < rowCount) {
							if (mData.getColumnSortList().getPrimaryColumn() != -1) {
								System.out.println("re-sorting due to addition");
								mData.sortColumn(mData.getColumnSortList().getPrimaryColumn(), mData.getColumnSortList().isPrimaryAscending());
							}
						}
						redraw();
					}
				});
			}

		}
	}

}
