package edu.washington.cs.oneswarm.ui.gwt.client.filebrowser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.gen2.table.client.FixedWidthFlexTable;
import com.google.gwt.gen2.table.client.FixedWidthGrid;
import com.google.gwt.gen2.table.client.ScrollTable;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ResizePolicy;
import com.google.gwt.gen2.table.client.AbstractScrollTable.ScrollPolicy;
import com.google.gwt.gen2.table.client.AbstractScrollTable.SortPolicy;
import com.google.gwt.gen2.table.client.SelectionGrid.SelectionPolicy;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.ReportableErrorDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.EntireUIRoot;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.OneSwarmCss;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileListLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ReportableException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;

public class UpdateSkippedFilesDialog extends OneSwarmDialogBox {

	public static final String CSS_F2F_TABS = "os-f2f_tabs";

	private List<CheckBox> checkboxes = new ArrayList<CheckBox>();
	private ScrollTable fileTable;
	private Button okButton = new Button(msg.button_update());

	private EntireUIRoot mRoot = null;
	private OneSwarmDialogBox mToClose = this;

	private boolean[] updated = null;

	private Label statusLabel = new Label(msg.update_files_loading());

	private FixedWidthGrid mFilesData = null;

	private static int WIDTH = 400;
	private DockPanel bottom = new DockPanel();

	private TorrentInfo torrent;
	private FileListLite[] files = null;
	final CheckBox streamCheck = new CheckBox(msg.update_files_streaming_download());
	boolean initialStream = false;
	private boolean[] initial_checks;

	public UpdateSkippedFilesDialog(EntireUIRoot inRoot, TorrentInfo inTorrent) {
		super(false);

		this.setText("Details: " + StringTools.truncate(inTorrent.getName(), 40, true));

		mRoot = inRoot;

		this.torrent = inTorrent;

		updated = new boolean[torrent.getNumFiles()];
		Arrays.fill(updated, false);

		initUI();

		OneSwarmRPCClient.getService().getFilesForDownloadingTorrentHash(OneSwarmRPCClient.getSessionID(), inTorrent.getTorrentID(), new AsyncCallback<FileListLite[]>() {
			public void onFailure(Throwable caught) {
				System.err.println(caught.toString());
				statusLabel.setText(caught.toString());
			}

			public void onSuccess(FileListLite[] result) {
				statusLabel.setText("");
				files = result;
				update();
			}
		});
	}

	private Button selectNoneButton = new Button(msg.button_select_none());
	private Button selectAllButton = new Button(msg.button_select_all());

	// private void completed() {
	// selectNoneButton.addClickHandler(new ClickHandler() {
	// public void onClick(ClickEvent event) {
	// for (CheckBox c : checkboxes) {
	// if (c.isEnabled()) {
	// c.setValue(false);
	// }
	// }
	// }
	// });
	//
	// selectAllButton.addClickHandler(new ClickHandler() {
	// public void onClick(ClickEvent event) {
	// for (CheckBox c : checkboxes) {
	// if (c.isEnabled()) {
	// c.setValue(true);
	// }
	// }
	// }
	// });
	//
	// HorizontalPanel f = new HorizontalPanel();
	// f.setSpacing(3);
	// f.add(selectAllButton);
	// f.add(selectNoneButton);
	// bottom.add(f, DockPanel.WEST);
	// bottom.setCellVerticalAlignment(f, DockPanel.ALIGN_MIDDLE);
	// bottom.setCellHorizontalAlignment(f, DockPanel.ALIGN_LEFT);
	//
	// }

	// public F2FTorrentDownloader(FileListLite[] files) {
	// this.collection = files;
	//
	// initUI();
	// update();
	// }

	private ClickHandler enableDisableDLButton = new ClickHandler() {
		public void onClick(ClickEvent event) {
			// if nothing is selected, we can't download
			boolean good = false;
			for (int cItr = 0; cItr < checkboxes.size(); cItr++) {
				CheckBox c = checkboxes.get(cItr);
				if (c.isEnabled() && ((c.getValue() == true && initial_checks[cItr] == false) || (c.getValue() == false && initial_checks[cItr] == true))) {
					good = true;
					break;
				}
			}
			
			if (good) {
				okButton.setEnabled(true);
			} else {
				okButton.setEnabled(false);
			}
			if(streamCheck.getValue() != initialStream){
				okButton.setEnabled(true);
			}
		}
	};
	DecoratedTabPanel tabs = new DecoratedTabPanel();

	private void initUI() {

		String[] dl_cols = new String[] { "DL", msg.swarm_browser_sort_name(), msg.swarm_browser_sort_size() };
		FixedWidthFlexTable filesHeader = new FixedWidthFlexTable();

		int[] widths = new int[] { 20, 300, 70 };

		mFilesData = new FixedWidthGrid(0, dl_cols.length);

		for (int i = 0; i < dl_cols.length; i++) {
			filesHeader.setText(0, i, dl_cols[i]);

			filesHeader.setColumnWidth(i, widths[i]);
			mFilesData.setColumnWidth(i, widths[i]);
		}

		mFilesData.setSelectionPolicy(SelectionPolicy.MULTI_ROW);

		fileTable = new ScrollTable(mFilesData, filesHeader);
		fileTable.setScrollPolicy(ScrollPolicy.DISABLED);
		fileTable.setResizePolicy(ScrollTable.ResizePolicy.FILL_WIDTH);
		fileTable.setSortPolicy(SortPolicy.DISABLED);

		mFilesData.addTableListener(new TableListener() {
			public void onCellClicked(SourcesTableEvents sender, int row, int cell) {
				CheckBox cb = (CheckBox) (mFilesData.getWidget(row, 0));
				if (cb.isEnabled()) {
					cb.setValue(!cb.getValue());
					UpdateSkippedFilesDialog.this.enableDisableDLButton.onClick(null);
				}
			}
		});

		VerticalPanel mainPanel = new VerticalPanel();
		VerticalPanel filesPanel = new VerticalPanel();

		Label selectLabel = new Label(msg.update_files_banner());
		selectLabel.addStyleName(CSS_DIALOG_HEADER);
		selectLabel.setWidth(WIDTH + "px");
		mainPanel.add(selectLabel);

		filesPanel.add(fileTable);

		filesPanel.add(streamCheck);
		streamCheck.addClickHandler(enableDisableDLButton);
		
		OneSwarmRPCClient.getService().isStreamingDownload(OneSwarmRPCClient.getSessionID(), torrent.getTorrentID(), new AsyncCallback<Boolean>() {
			public void onFailure(Throwable caught) {
			}

			public void onSuccess(Boolean result) {
				if (result != null && result.booleanValue() == true) {
					streamCheck.setValue(true);
				}
				initialStream = result;
			}
		});
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setSpacing(3);
		Button cancelButton = new Button(msg.button_cancel());
		cancelButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		// cancelButton.setWidth("100px");

		buttonPanel.add(cancelButton);

		// okButton.setWidth("100px");
		okButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {

				okButton.setEnabled(false);
				for (int i = 0; i < checkboxes.size(); i++) {
					checkboxes.get(i).setEnabled(false);
					files[i].setSkipped(!checkboxes.get(i).getValue());
				}

				statusLabel.setText(msg.update_files_updating());

				OneSwarmRPCClient.getService().updateSkippedFiles(OneSwarmRPCClient.getSessionID(), files, new AsyncCallback<ReportableException>() {
					public void onFailure(Throwable caught) {
						System.err.println(caught);
						caught.printStackTrace();
					}

					public void onSuccess(ReportableException result) {
						if (result != null) {
							new ReportableErrorDialogBox(result, false);
						} else {
							mRoot.refreshSwarms();

							mToClose.hide();
						}
					}
				});
				OneSwarmRPCClient.getService().setStreamingDownload(OneSwarmRPCClient.getSessionID(), torrent.getTorrentID(), streamCheck.getValue(), new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
					}

					public void onSuccess(Void result) {
					}
				});

			}
		});

		okButton.setEnabled(false);

		buttonPanel.add(okButton);

		buttonPanel.setCellHorizontalAlignment(okButton, HorizontalPanel.ALIGN_RIGHT);
		buttonPanel.setCellHorizontalAlignment(cancelButton, HorizontalPanel.ALIGN_RIGHT);
		buttonPanel.setCellHorizontalAlignment(statusLabel, HorizontalPanel.ALIGN_LEFT);

		bottom.setWidth("100%");
		bottom.add(buttonPanel, DockPanel.EAST);
		bottom.setCellHorizontalAlignment(buttonPanel, HorizontalPanel.ALIGN_RIGHT);

		bottom.add(statusLabel, DockPanel.WEST);
		bottom.setCellVerticalAlignment(statusLabel, VerticalPanel.ALIGN_MIDDLE);
		bottom.setCellHorizontalAlignment(statusLabel, HorizontalPanel.ALIGN_LEFT);

		VerticalPanel sourcesPanel = new VerticalPanel();

		sourcesPanel.add(new Label(msg.torrent_download_sources_not_available()));

		tabs.addStyleName(CSS_F2F_TABS);
		tabs.add(filesPanel, msg.settings_tab_files());
		tabs.add(sourcesPanel, msg.torrent_download_tab_sources());

		tabs.setWidth(WIDTH + "px");
		tabs.addTabListener(new TabListener() {

			public boolean onBeforeTabSelected(SourcesTabEvents arg0, int tabIndex) {
				return true;
			}

			public void onTabSelected(SourcesTabEvents arg0, int tabIndex) {
				if (tabIndex == 0) {
					selectAllButton.setVisible(true);
					selectNoneButton.setVisible(true);
				} else {
					selectAllButton.setVisible(false);
					selectNoneButton.setVisible(false);
				}
			}
		});
		mainPanel.add(tabs);

		mainPanel.add(bottom);

		mainPanel.setWidth(WIDTH + "px");

		tabs.selectTab(0);

		this.setWidget(mainPanel);
	}

	public void hide() {
		OneSwarmGWT.log("removing from update task");
		super.hide();
	}

	private void update() {
		// OneSwarmGWT.log("running update");
		// fileTable.setVisible(false);
		mFilesData.clear();
		checkboxes.clear();

		initial_checks = new boolean[files.length];

		if (mFilesData.getRowCount() != torrent.getNumFiles()) {
			mFilesData.resize(torrent.getNumFiles(), 3);
		}
		for (int i = 0; i < torrent.getNumFiles(); i++) {
			final CheckBox b = new CheckBox();

			b.addClickHandler(enableDisableDLButton);
			b.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					b.setValue(!b.getValue());
				}
			});

			b.setValue(!files[i].isSkipped());
			b.setEnabled(!files[i].isFinishedDL());

			this.initial_checks[i] = !files[i].isSkipped();

			checkboxes.add(b);
			mFilesData.setWidget(i, 0, b);
			mFilesData.setText(i, 1, files[i].getFileName());
			mFilesData.setText(i, 2, StringTools.formatRate(files[i].getFileSize()));

			// OneSwarmGWT.log("adding: " + f.toString());
		}

		MediaInfoPanel mediaInfo = new MediaInfoPanel();
		tabs.add(mediaInfo, "File Info");
		mediaInfo.update(files);

		fileTable.redraw();

	}

	static class MediaInfoPanel extends VerticalPanel {
		private final ScrollTable table;
		final ListBox defaultActionMenu = new ListBox();
		FileListLite[] files;
		String[] header = new String[] { "Key", "Value" };
		FixedWidthGrid dataTable;
		Button viewFFMpegOutButton = new Button("Ffmpeg output");

		public MediaInfoPanel() {
			table = new ScrollTable(new FixedWidthGrid(1, 2), new FixedWidthFlexTable());
			table.setScrollPolicy(ScrollPolicy.DISABLED);
			table.setResizePolicy(ResizePolicy.FILL_WIDTH);

			FixedWidthFlexTable headerTable = table.getHeaderTable();
			dataTable = table.getDataTable();

			for (int i = 0; i < header.length; i++) {
				headerTable.setWidget(0, i, new Label(header[i]));
				dataTable.setWidget(0, i, new Label("loading..."));
			}
			defaultActionMenu.addItem("loading...");
			add(defaultActionMenu);
			add(table);
			add(viewFFMpegOutButton);
			viewFFMpegOutButton.setVisible(false);
			viewFFMpegOutButton.addStyleName(OneSwarmCss.SMALL_BUTTON);
			viewFFMpegOutButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					OneSwarmDialogBox dlg = new OneSwarmDialogBox(true, true, true);
					dlg.hide();
					dlg.setText("FFMpeg debug output");
					TextArea a = new TextArea();
					a.setReadOnly(true);
					a.addStyleName("os-add_friend_publickey_text");
					a.setText(ffmpegOut);
					dlg.setWidget(a);
					a.setWidth("800px");
					a.setHeight("600px");
					dlg.center();
					dlg.show();
				}
			});
			defaultActionMenu.addChangeHandler(new ChangeHandler() {
				public void onChange(ChangeEvent event) {
					int selected = Integer.parseInt(defaultActionMenu.getValue(defaultActionMenu.getSelectedIndex()));
					FileListLite f = files[selected];
					System.out.println("getting media info for: " + f.getCollectionId() + " " + f.getFileName());
					dataTable.resizeRows(1);
					for (int i = 0; i < header.length; i++) {
						dataTable.setWidget(0, i, new Label("loading..."));
					}
					updateTable(f);
				}
			});
		}

		public void update(FileListLite[] files) {
			this.files = files;
			defaultActionMenu.clear();
			for (int i = 0; i < files.length; i++) {
				FileListLite fileListLite = files[i];
				defaultActionMenu.addItem(fileListLite.getFileName(), i + "");
			}
			if (defaultActionMenu.getItemCount() > 0) {
				defaultActionMenu.setSelectedIndex(0);
				updateTable(files[0]);
			}

		}

		String ffmpegOut;

		public void updateTable(FileListLite file) {

			OneSwarmRPCClient.getService().getFileInfo(OneSwarmRPCClient.getSessionID(), file, file.isFinishedDL() && OneSwarmConstants.InOrderType.getType(file.getFileName()) != null, new AsyncCallback<HashMap<String, String>>() {
				public void onFailure(Throwable caught) {
					System.err.println(caught);
					caught.printStackTrace();
				}

				public void onSuccess(HashMap<String, String> result) {
					ArrayList<String> keys = new ArrayList<String>();
					ffmpegOut = null;
					for (String k : result.keySet()) {
						if (k.equals("ffmpegOut")) {
							ffmpegOut = result.get(k);
						} else {
							keys.add(k);
						}
					}
					Collections.sort(keys);

					dataTable.resizeRows(keys.size());
					for (int i = 0; i < keys.size(); i++) {
						String k = keys.get(i);
						dataTable.setWidget(i, 0, new Label(k));
						final String hash = result.get(k);
						dataTable.setWidget(i, 1, new Label(hash));

						if (k.contains("sha1") || k.contains("ed2k")) {
							final String searchString = k.replaceFirst("hash_", "").trim() + ";" + hash;
							HTML h = new HTML("<a href=#" + EntireUIRoot.SEARCH_HISTORY_TOKEN + searchString + ">" + hash + "</a>");
							h.addClickHandler(new ClickHandler() {
								public void onClick(ClickEvent event) {
									EntireUIRoot.getRoot(MediaInfoPanel.this).displaySearch(searchString);
								}
							});
							dataTable.setWidget(i, 1, h);
						}

					}

					viewFFMpegOutButton.setVisible(ffmpegOut != null);
					table.redraw();
				}
			});
		}
	}
}
