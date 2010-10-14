package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.gen2.table.event.client.RowSelectionEvent;
import com.google.gwt.gen2.table.event.client.RowSelectionHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.widgetideas.client.ProgressBar;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.Updateable;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendsDetailsTable.HeaderWithWidth;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class SwarmsListTable extends ScrollTable implements Updateable {

	FixedWidthGrid mData = null;
	FixedWidthFlexTable mHeader = null;

	List<Integer> mFilteredRows = new ArrayList<Integer>();

	TorrentInfo[] mSwarms = null;
	// private SwarmsBrowser mSwarmsBrowser;
	
	protected static OSMessages msg = OneSwarmGWT.msg;

	private static final HeaderWithWidth[] COLUMNS = new HeaderWithWidth[] { new HeaderWithWidth("", 20),

	new HeaderWithWidth(msg.swarm_browser_sort_name(), 300, true), new HeaderWithWidth(msg.settings_tab_files(), 25),

	new HeaderWithWidth(msg.swarm_browser_sort_size(), 25), new HeaderWithWidth(msg.add_friends_invite_view_table_status(), 50),

	new HeaderWithWidth(msg.swarm_browser_sort_date(), 40) };

	public SwarmsListTable(TorrentInfo[] swarms, final SwarmsBrowser swarmsBrowser) {
		super(new FixedWidthGrid(0, COLUMNS.length - 1) {
			@Override
			protected int getInputColumnWidth() {
				return COLUMNS[0].width;
			}
		}, new FixedWidthFlexTable());

		mSwarms = swarms;
		// mSwarmsBrowser = swarmsBrowser;

		mData = getDataTable();
		mHeader = getHeaderTable();

		/**
		 * We do sorting on the backend via the menu
		 */

		this.setSortPolicy(SortPolicy.DISABLED);
		mData.setSelectionPolicy(SelectionPolicy.CHECKBOX);

		mData.addTableListener(new TableListener() {
			public void onCellClicked(SourcesTableEvents sender, int row, int cell) {
				if (mData.isRowSelected(row)) {
					mData.deselectRow(row);
				} else {
					mData.selectRow(row, false);
				}
				swarmsBrowser.refreshHeaderButtons();
				swarmsBrowser.propAction();
			}
		});
		mData.addRowSelectionHandler(new RowSelectionHandler() {
			public void onRowSelection(RowSelectionEvent event) {
				swarmsBrowser.refreshHeaderButtons();
				swarmsBrowser.propAction();
			}
		});

		setScrollPolicy(ScrollPolicy.DISABLED);
		setResizePolicy(ResizePolicy.FILL_WIDTH);

		setupHeader();
		addSwarms();
		resizeTable();
		
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
		// mData.setColumnWidth(cItr, (int) Math.round((double)
		// mData.getColumnWidth(cItr) * factor));
		// }
		// }
		// lastWindowWidth = Window.getClientWidth();
		// }
		// });
	}

	private void resizeTable() {
		for (int i = 0; i < COLUMNS.length; i++) {
			mHeader.setColumnWidth(i, COLUMNS[i].width);
			if (i < COLUMNS.length - 1) {
				mData.setColumnWidth(i, COLUMNS[i + 1].width);
			}
		}
		fillWidth();
	}

	public void onAttach() {
		super.onAttach();
		OneSwarmGWT.addToUpdateTask(this);
	}

	public void onDetach() {
		super.onDetach();
		OneSwarmGWT.removeFromUpdateTask(this);
	}

	class EncapsulatingLabel extends Label {
		public final static int COLUMN = 0;
		public TorrentInfo swarm;
		public ProgressBar progress;

		public EncapsulatingLabel(TorrentInfo swarm) {
			super(swarm.getName());
			this.swarm = swarm;
		}
	}

	private void addSwarms() {

		if (mData.getRowCount() != mSwarms.length) {
			mData.resizeRows(mSwarms.length);
		}
		for (int row = 0; row < mSwarms.length; row++) {
			final TorrentInfo swarm = mSwarms[row];

			EncapsulatingLabel encap = new EncapsulatingLabel(swarm);
			int col = 0;
			mData.setWidget(row, EncapsulatingLabel.COLUMN, encap);
			col++;
			mData.setText(row, col, Integer.toString(swarm.getNumFiles()));
			col++;
			mData.setText(row, col, StringTools.formatRate(swarm.getTotalSize()));
			col++;
			if (swarm.isF2FOnly()) {
				mData.setText(row, col, "F2F (" + swarm.getF2F_nick() + ")");
			} else if (swarm.getProgress() == 1000) {
				mData.setText(row, col, "Complete");
			} else {
				encap.progress = new ProgressBar(0, 1000);
				encap.progress.setTextFormatter(new SwarmRateTextFormatter(swarm));
				encap.progress.setProgress(swarm.getProgress());
				mData.setWidget(row, col, encap.progress);
			}
			col++;
			mData.setText(row, col, StringTools.formatDateMonthDayYear(new Date(swarm.getAddedDate())));
		}
	}

	private void setupHeader() {

		for (int i = 0; i < COLUMNS.length; i++) {
			mHeader.setText(0, i, COLUMNS[i].name);
		}
	}

	public void update(int count) {
		ArrayList<String> whichOnes = new ArrayList<String>();

		final HashMap<String, TorrentInfo> id_to_swarm = new HashMap<String, TorrentInfo>();
		for (TorrentInfo s : mSwarms) {
			whichOnes.add(s.getTorrentID());
			id_to_swarm.put(s.getTorrentID(), s);
		}

		OneSwarmRPCClient.getService().pagedTorrentStateRefresh(OneSwarmRPCClient.getSessionID(), whichOnes, new AsyncCallback<TorrentInfo[]>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(TorrentInfo[] result) {
				for (TorrentInfo updated : result) {
					if (updated == null) {
						continue;
					}

					if (updated.getTorrentID() == null) {
						System.err.println("torrent ID is null: " + updated.getName());
						continue;
					}
					TorrentInfo existing = id_to_swarm.get(updated.getTorrentID());
					if (existing == null) {
						System.err.println("SwarmsListTable: couldn't get existing for new swarm info: " + updated.getName());
						continue;
					}

					existing.setProgress(updated.getProgress());
					existing.setUploadRate(updated.getUploadRate());
					existing.setDownloadRate(updated.getDownloadRate());
					existing.setDownloaded(updated.getDownloaded());
					existing.setStatus(updated.getStatus());
				}

				// now refresh UI
				for (int row = 0; row < mData.getRowCount(); row++) {
					EncapsulatingLabel encap = ((EncapsulatingLabel) mData.getWidget(row, EncapsulatingLabel.COLUMN));
					if (encap.progress != null) {
						if (encap.swarm.getProgress() == 1000) {
							encap.progress.removeFromParent();
							mData.setText(row, 4, "Complete");
						} else {
							encap.progress.setProgress(((EncapsulatingLabel) mData.getWidget(row, EncapsulatingLabel.COLUMN)).swarm.getProgress());
						}
					}
				}
			}
		});
	}

	public void checkAll(boolean check) {
		if (check) {
			mData.selectAllRows();
		} else {
			mData.deselectAllRows();
		}
	}

	public TorrentInfo[] getSelectedSwarms() {

		List<TorrentInfo> out = new ArrayList<TorrentInfo>();
		for (int row = 0; row < mData.getRowCount(); row++) {
			EncapsulatingLabel label = ((EncapsulatingLabel) mData.getWidget(row, 0));
			if (mData.isRowSelected(row)) {
				out.add(label.swarm);
			}
		}
		if (out.size() > 0)
			return out.toArray(new TorrentInfo[0]);
		else
			return null;
	}
}
