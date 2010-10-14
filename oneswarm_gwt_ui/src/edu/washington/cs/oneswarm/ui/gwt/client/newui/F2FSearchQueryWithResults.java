/**
 * 
 */
package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ColumnResizePolicy;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ScrollPolicy;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.filebrowser.TorrentDownloaderDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.filebrowser.UpdateSkippedFilesDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.SettingsDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.tableutils.SortableInt;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.tableutils.SortableSizeColumn;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.transfer_details.TransferColumnSorter;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileListLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIServiceAsync;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TextSearchResultLite;

/**
 * @author isdal
 * 
 */
public class F2FSearchQueryWithResults extends HorizontalPanel implements Updateable {

	public interface ResultsCallback {
		public void updateCount(int count);
	}

	ScrollTable resultsTable = null;
	private final int searchId;
	private TextSearchResultLite[] tableContent = new TextSearchResultLite[0];

	private FixedWidthGrid mResultsData = null;

	private HTML mStatusLabel = null;
	private static final double FAKE_DONE_MAX_TIME_MS = 30 * 1000;
	private static final double SEARCH_EXPIRE_TIME = 4.5 * 60 * 1000;

	private static class TextSearchResultLiteAwareRow extends Label {
		TextSearchResultLite tsr;

		public TextSearchResultLiteAwareRow(String label, TextSearchResultLite tsr) {
			super(label);
			this.tsr = tsr;
		}

		public TextSearchResultLite getSearchResult() {
			return tsr;
		}
	}

	FixedWidthFlexTable mHeader;
	private ResultsCallback callback;
	private final long createdTime;
	private final String keywords;

	private int maxSearchResults = 500;

	public F2FSearchQueryWithResults(String keywords, final EntireUIRoot inRoot, int searchId, HTML statusLabel) {
		this(keywords, inRoot, searchId, statusLabel, null);
	}

	public F2FSearchQueryWithResults(String keywords, final EntireUIRoot inRoot, int searchId, HTML statusLabel, ResultsCallback callback) {
		this.keywords = keywords;
		this.createdTime = System.currentTimeMillis();
		this.searchId = searchId;

		this.callback = callback;

		mStatusLabel = statusLabel;

		this.setWidth("100%");

		resultsTable = new ScrollTable(new FixedWidthGrid(0, Strings.F2F_RESULTS_COLS.length), new FixedWidthFlexTable());
		this.add(resultsTable);

		mResultsData = resultsTable.getDataTable();
		mHeader = resultsTable.getHeaderTable();
		mResultsData.setColumnSorter(new TransferColumnSorter());

		resultsTable.setScrollPolicy(ScrollPolicy.DISABLED);
		resultsTable.setResizePolicy(ScrollTable.ResizePolicy.FILL_WIDTH);

		for (int i = 0; i < Strings.F2F_RESULTS_COLS.length; i++) {
			mHeader.setText(0, i, Strings.F2F_RESULTS_COLS[i]);
		}

		mResultsData.setSelectionPolicy(SelectionPolicy.ONE_ROW);
		// mResultsData.setSelectionPolicy(SelectionPolicy.CHECKBOX);
		resizeTable();
		update(0);
		OneSwarmGWT.addToUpdateTask(this);

		mResultsData.addTableListener(new TableListener() {
			public void onCellClicked(SourcesTableEvents arg0, int row, int arg2) {
				Set<Integer> selectedRows = mResultsData.getSelectedRows();
				// for (Integer row : selectedRows) {

				OneSwarmGWT.log("clicked: row=" + row);
				// TextSearchResultLite s = tableContent[row];

				TextSearchResultLite s = ((TextSearchResultLiteAwareRow) mResultsData.getWidget(row, 0)).getSearchResult();
				if (s.isInLibrary()) {
					UpdateSkippedFilesDialog dlg = new UpdateSkippedFilesDialog(EntireUIRoot.getRoot(F2FSearchQueryWithResults.this), s.getTorrentInfo());

					dlg.show();
					dlg.setVisible(false);
					dlg.center();
					dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
					dlg.setVisible(true);
				} else {
					long age = System.currentTimeMillis() - createdTime;
					FileListLite clickedRow = new FileListLite(s.getCollectionId(), s.getCollectionName(), s.getFileName(), s.getFileSize(), 0, s.getAddedTimeUTC(), 0, false, false);
					OneSwarmDialogBox dlg;
					if (age > SEARCH_EXPIRE_TIME) {
						dlg = new SearchExpiredDialog(clickedRow);
					} else {
						dlg = new TorrentDownloaderDialog(inRoot, null, s.getConnectionId(), s.getChannelId(), 0, clickedRow, false, s.getThroughFriends(), s.getFriendDelay());
					}
					dlg.show();
					dlg.setVisible(false);
					dlg.center();
					dlg.setPopupPosition(dlg.getPopupLeft(), Window.getScrollTop() + 125);
					dlg.setVisible(true);
				}
				// }
			}
		});

		// Window.addWindowResizeListener(new WindowResizeListener() {
		//
		// int lastWindowWidth = Window.getClientWidth();
		//
		// public void onWindowResized(int width, int height) {
		// System.out.println("this width: " + Window.getClientWidth() +
		// " last: " + lastWindowWidth);
		// /**
		// * For some reason, we need to do this manually.
		// */
		// if (Window.getClientWidth() < lastWindowWidth) {
		// double factor = (double) Window.getClientWidth() / (double)
		// lastWindowWidth;
		// factor *= 0.95; // take care of slop. eventually, this will
		// // resize anyway to fill width when we
		// // update. HACK ATTACK!
		// System.out.println("factor: " + factor);
		// for (int cItr = 0; cItr < mHeader.getColumnCount(); cItr++) {
		// mHeader.setColumnWidth(cItr, (int) Math.round((double)
		// mHeader.getColumnWidth(cItr) * factor));
		// mResultsData.setColumnWidth(cItr, (int) Math.round((double)
		// mResultsData.getColumnWidth(cItr) * factor));
		// }
		// }
		// lastWindowWidth = Window.getClientWidth();
		// }
		// });
	}

	// private String[] header = { "Delay", "File name", "File size",
	// "Torrent name", "Through friends" };

	// private void addHeader() {
	// for (int i = 0; i < header.length; i++) {
	// String h = header[i];
	// table.setHTML(0, i, "<b>" + h + "</b>");
	// }
	// }

	private void resizeTable() {
		int[] widths = new int[] { 500, 60, 250, 60, 40, 50 };
		for (int i = 0; i < Strings.F2F_RESULTS_COLS.length; i++) {
			mHeader.setColumnWidth(i, widths[i]);
			mResultsData.setColumnWidth(i, widths[i]);
			// resultsTable.setColumnWidth(i, widths[i]);
			// cols 0 and 2 expand
			if (i != 0 && i != 2) {
				resultsTable.setMaximumColumnWidth(i, widths[i]);
			}
		}

		resultsTable.fillWidth();
	}

	private void update() {

		mResultsData.setVisible(false);
		mResultsData.clearAll();

		mResultsData.resizeRows(tableContent.length);

		for (int i = 0; i < tableContent.length; i++) {
			TextSearchResultLite res = tableContent[i];

			// mResultsData.setText(i + 1, 0, res.getAge() + "ms");
			Label nameLabel = new TextSearchResultLiteAwareRow(res.getFileName(), res);
			nameLabel.setTitle(res.getFileName());

			Label groupLabel = new Label(res.getCollectionName());
			groupLabel.setTitle(res.getCollectionName());

			mResultsData.setWidget(i, 0, nameLabel);
			nameLabel.addStyleName(OneSwarmCss.CLICKABLE);

			SortableSizeColumn size = new SortableSizeColumn(res.getFileSize());
			mResultsData.setWidget(i, 1, size);
			size.addStyleName(OneSwarmCss.CLICKABLE);

			mResultsData.setWidget(i, 2, groupLabel);
			groupLabel.addStyleName(OneSwarmCss.CLICKABLE);

			SortableInt paths = new SortableInt(res.getThroughFriends().length);
			mResultsData.setWidget(i, 3, paths);
			paths.addStyleName(OneSwarmCss.CLICKABLE);

			if (mResultsData.getColumnCount() > 4) {
				Long[] delays = res.getFriendDelay();
				SortableInt delay = new SortableInt(delays[0].intValue());
				mResultsData.setWidget(i, 4, delay);
				paths.addStyleName(OneSwarmCss.CLICKABLE);

			}
			if (mResultsData.getColumnCount() > 5) {
				mResultsData.setWidget(i, 5, new Label(res.getCollectionId()));
				paths.addStyleName(OneSwarmCss.CLICKABLE);
			}
		}

		mResultsData.setVisible(true);

		resultsTable.redraw();
	}

	public void update(int count) {

		String session = OneSwarmRPCClient.getSessionID();
		OneSwarmUIServiceAsync service = OneSwarmRPCClient.getService();
		service.getIntegerParameterValue(session, "oneswarm.max.ui.search.results", new AsyncCallback<Integer>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Integer result) {
				maxSearchResults = result.intValue();
			}
		});

		AsyncCallback<TextSearchResultLite[]> cb = new AsyncCallback<TextSearchResultLite[]>() {
			public void onFailure(Throwable caught) {
				// well, do nothing...
				OneSwarmGWT.log("error " + caught.getMessage());
			}

			public void onSuccess(TextSearchResultLite[] result) {

				OneSwarmGWT.log("searchquery: got " + result.length + " rows");
				long age = System.currentTimeMillis() - createdTime;
				String newStatusHtml = "";
				if (mStatusLabel != null) {
					long fakeDone = Math.round(Math.min(100, 100 * age / FAKE_DONE_MAX_TIME_MS));
					if (result.length >= maxSearchResults) {
						newStatusHtml = ("Search completed, got more than " + maxSearchResults + " responses, showing 1-" + result.length + " (<a href='#'>change limit</a>)");
						stopUpdates();
						mStatusLabel.addClickHandler(new ClickHandler() {

							public void onClick(ClickEvent event) {
								final EntireUIRoot root = EntireUIRoot.getRoot(F2FSearchQueryWithResults.this);
								SettingsDialog dlg = new SettingsDialog(root, root.swarmFileBrowser, 2);
							}
						});

					} else if (fakeDone == 100) {
						if (result.length == 0) {
							newStatusHtml = ("Search completed, no results found matching '" + keywords + "'");
							stopUpdates();
						} else {
							newStatusHtml = ("Search completed, " + result.length + " results found matching '" + keywords + "'");
							stopUpdates();
						}
					} else {
						newStatusHtml = ("Searching for '" + keywords + "' (" + fakeDone + "%)");
					}

				}

				if (result.length > tableContent.length) {
					tableContent = result;
					update();
				}

				if (callback != null) {
					callback.updateCount(result.length);
				}

				if (age > SEARCH_EXPIRE_TIME) {
					newStatusHtml = "Search results expired";
					stopUpdates();
				}
				if (!mStatusLabel.getHTML().equals(newStatusHtml)) {
					mStatusLabel.setHTML(newStatusHtml);
				}
			}
		};
		service.getSearchResult(session, searchId, cb);
	}

	public void onDetach() {
		super.onDetach();
		stopUpdates();
	}

	public void stopUpdates() {
		OneSwarmGWT.removeFromUpdateTask(this);
	}

	class SearchExpiredDialog extends OneSwarmDialogBox {
		public SearchExpiredDialog(final FileListLite clickedRow) {
			super(false, true, true);
			super.setText("Search expired");
			VerticalPanel mainPanel = new VerticalPanel();
			super.setWidget(mainPanel);

			VerticalPanel labelPanel = new VerticalPanel();
			labelPanel.addStyleName("os-NotificationDialogBox");
			labelPanel.setSpacing(7);
			mainPanel.add(labelPanel);

			Label selectLabel = new HTML("<b>This search has expired</b>");
			labelPanel.add(selectLabel);
			Label row1 = new HTML("Click <i>Search again</i> to search for:");
			labelPanel.add(row1);
			Label row2 = new HTML("<i>" + clickedRow.getFileName() + "</i>");
			labelPanel.add(row2);

			HorizontalPanel buttonPanel = new HorizontalPanel();
			mainPanel.add(buttonPanel);
			mainPanel.setCellHorizontalAlignment(buttonPanel, VerticalPanel.ALIGN_RIGHT);
			buttonPanel.setSpacing(5);
			buttonPanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);

			Button cancelButton = new Button("Cancel");
			buttonPanel.add(cancelButton);
			cancelButton.addClickListener(new ClickListener() {
				public void onClick(Widget sender) {
					SearchExpiredDialog.this.hide();
				}
			});

			Button searchButton = new Button("Search again");
			buttonPanel.add(searchButton);
			searchButton.addClickListener(new ClickListener() {
				public void onClick(Widget sender) {
					String fileName = clickedRow.getFileName();
					EntireUIRoot.getRoot(F2FSearchQueryWithResults.this).displaySearch(fileName.substring(0, Math.min(fileName.length(), 100)) + " id:" + clickedRow.getCollectionId());
					SearchExpiredDialog.this.hide();
				}
			});

		}
	}
}
