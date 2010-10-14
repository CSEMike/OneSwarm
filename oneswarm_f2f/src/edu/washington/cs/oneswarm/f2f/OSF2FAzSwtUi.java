package edu.washington.cs.oneswarm.f2f;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.peer.impl.transport.PEPeerTransportProtocol;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.UnloadablePlugin;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.utils.Formatters;
import org.gudy.azureus2.pluginsimpl.local.peers.PeerImpl;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import edu.washington.cs.oneswarm.f2f.network.FriendConnection;
import edu.washington.cs.oneswarm.f2f.network.OverlayTransport;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection.OverlayForward;

public class OSF2FAzSwtUi {
	public static final String KEY_OVERLAY_TRANSPORT = "key_peer_transport";
	// public static final String KEY_PEER_IP = "key_peer_descr";
	// public static final String KEY_PEER_PATH_ID = "key_peer_path_id";

	static final String COLUMNID_VIA_FRIEND = "Via Friend";
	static final String COLUMNID_ADDED_DELAY = "Added delay";
	static final String COLUMNID_FRIEND_IP = "Friend ip:port";
	static final String COLUMNID_PATH_ID = "Path ID";
	static final String COLUMNID_CONNECTION_WEIGHT = "Connection weight";

	private static final String MENUID_MAKE_F2F_ONLY = "Set F2F Only";
	private static final String MENUID_ENABLE_ALL_NETWORKS = "Enable all networks";
	private static final String MENUID_MAKE_F2F_ONLY_WHEN_DONE = "Set F2F only (when done)";
	Formatters formatter;
	private final OSF2FMain main;

	public OSF2FAzSwtUi(OSF2FMain main) {
		this.main = main;
	}

	/**
	 * This method is called when the plugin is loaded / initialized
	 * 
	 * @param pluginInterface
	 *            access to Azureus' plugin interface
	 */
	public void initialize(PluginInterface pluginInterface) {
		// Get the Formatters object to be used when setting the cell's text
		formatter = pluginInterface.getUtilities().getFormatters();
		// Of course, we'll need the TableManager object in order to add a
		// column!
		TableManager tableManager = pluginInterface.getUIManager().getTableManager();

		addViaPeerColumn(tableManager);
		addRemoteIpColumn(tableManager);
		addPathIDColumn(tableManager);
		addArtificialDelayColumn(tableManager);
		addConnectionWeightColumn(tableManager);

		try {
			new FriendConnectionsView().initialize(pluginInterface);
		} catch (PluginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// MenuManager menuManager =
		// pluginInterface.getUIManager().getMenuManager();
		// createF2FOnlyMenu(menuManager);
		// createF2FOnlyWhenDoneMenu(menuManager);
		// createSetPublicMenu(menuManager);
	}

	// private void createF2FOnlyWhenDoneMenu(MenuManager menuManager) {
	// final MenuItem item2 =
	// menuManager.addMenuItem(MenuManager.MENU_DOWNLOAD_CONTEXT,
	// MENUID_MAKE_F2F_ONLY_WHEN_DONE);
	//
	// item2.setText("Set F2F only when done");
	// item2.setData(false);
	// item2.setStyle(MenuItem.STYLE_CHECK);
	// item2.addListener(new MenuItemListener() {
	// public void selected(MenuItem menu, Object target) {
	// boolean checked = (Boolean) menu.getData();
	// if (target instanceof DownloadImpl) {
	// DownloadImpl d = (DownloadImpl) target;
	// if (checked) {
	// d.addListener(f2fWhenDoneListener);
	// } else {
	// d.removeListener(f2fWhenDoneListener);
	// }
	// }
	// }
	// });
	// }
	//
	// private void createF2FOnlyMenu(MenuManager menuManager) {
	// final MenuItem item =
	// menuManager.addMenuItem(MenuManager.MENU_DOWNLOAD_CONTEXT,
	// MENUID_MAKE_F2F_ONLY);
	//
	// item.setText("Set F2F only");
	// // item.setData(false);
	// // item.setStyle(MenuItem.STYLE_CHECK);
	// item.addListener(new MenuItemListener() {
	// public void selected(MenuItem menu, Object target) {
	// if (target instanceof DownloadImpl) {
	// DownloadImpl d = (DownloadImpl) target;
	// main.getF2DownloadManager().setTorrentPrivacy(d.getTorrent().getHash(),
	// false, true);
	// }
	// }
	// });
	// }
	//
	// private void createSetPublicMenu(MenuManager menuManager) {
	// final MenuItem item =
	// menuManager.addMenuItem(MenuManager.MENU_DOWNLOAD_CONTEXT,
	// MENUID_ENABLE_ALL_NETWORKS);
	//
	// item.setText("Enable all networks");
	// // item.setData(false);
	// // item.setStyle(MenuItem.STYLE_CHECK);
	// item.addListener(new MenuItemListener() {
	// public void selected(MenuItem menu, Object target) {
	// if (target instanceof DownloadImpl) {
	// DownloadImpl d = (DownloadImpl) target;
	// main.getF2DownloadManager().setTorrentPrivacy(d.getTorrent().getHash(),
	// true, true);
	// }
	// }
	// });
	// }
	//
	// private DownloadListener f2fWhenDoneListener = new DownloadListener() {
	// public void positionChanged(Download download, int oldPosition, int
	// newPosition) {
	// }
	//
	// public void stateChanged(Download download, int old_state, int new_state)
	// {
	// if (old_state == Download.ST_DOWNLOADING && new_state ==
	// Download.ST_SEEDING) {
	// main.getF2DownloadManager().setTorrentPrivacy(download.getTorrent().getHash(),
	// false, true);
	// try {
	// download.stop();
	// } catch (DownloadException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// }
	// };

	private void addViaPeerColumn(TableManager tableManager) {
		TableColumn column;
		/*
		 * Add via peer column
		 */
		column = tableManager.createColumn(TableManager.TABLE_TORRENT_PEERS, COLUMNID_VIA_FRIEND);
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150, TableColumn.INTERVAL_LIVE);
		column.addCellRefreshListener(new TableCellRefreshListener() {
			public void refresh(TableCell cell) {
				PeerImpl peer = (PeerImpl) cell.getDataSource();
				if (peer == null) {
					cell.setText("");
					return;
				}

				OverlayTransport tr = (OverlayTransport) peer.getPEPeer().getData(KEY_OVERLAY_TRANSPORT);
				if (tr == null) {
					cell.setText("");
					return;
				}
				Friend f = tr.getRemoteFriend();
				if (f == null) {
					cell.setText("");
					return;
				}
				if (!cell.setSortValue(f.getNick() + ":") && cell.isValid()) {
					return;
				}
				cell.setText(f.getNick());
			}
		});
		tableManager.addColumn(column);
		/* done via peer column */
	}

	private void addArtificialDelayColumn(TableManager tableManager) {
		TableColumn column;
		/*
		 * Add delay column
		 */
		column = tableManager.createColumn(TableManager.TABLE_TORRENT_PEERS, COLUMNID_ADDED_DELAY);
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 150, TableColumn.INTERVAL_LIVE);
		column.addCellRefreshListener(new TableCellRefreshListener() {
			public void refresh(TableCell cell) {

				PeerImpl peer = (PeerImpl) cell.getDataSource();
				if (peer == null) {
					cell.setText("");
					return;
				}
				OverlayTransport tr = (OverlayTransport) peer.getPEPeer().getData(KEY_OVERLAY_TRANSPORT);
				if (tr == null) {
					cell.setText("");
					return;
				}

				if (!cell.setSortValue(tr.getArtificialDelay() + ":") && cell.isValid()) {
					return;
				}
				cell.setText(tr.getArtificialDelay() + "");
			}
		});
		tableManager.addColumn(column);
		/* done delay column */
	}

	private void addRemoteIpColumn(TableManager tableManager) {
		TableColumn column;

		column = tableManager.createColumn(TableManager.TABLE_TORRENT_PEERS, COLUMNID_FRIEND_IP);
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 100, TableColumn.INTERVAL_LIVE);
		column.addCellRefreshListener(new TableCellRefreshListener() {
			public void refresh(TableCell cell) {

				PeerImpl peer = (PeerImpl) cell.getDataSource();
				if (peer == null) {
					cell.setText("");
					return;
				}

				OverlayTransport tr = (OverlayTransport) peer.getPEPeer().getData(KEY_OVERLAY_TRANSPORT);
				if (tr == null) {
					cell.setText("");
					return;
				}
				String f = tr.getRemoteIP();
				if (f == null) {
					cell.setText("");
					return;
				}
				if (!cell.setSortValue(f + ":") && cell.isValid()) {
					return;
				}

				cell.setText(f);
			}
		});
		tableManager.addColumn(column);

	}

	private void addPathIDColumn(TableManager tableManager) {
		TableColumn column;

		column = tableManager.createColumn(TableManager.TABLE_TORRENT_PEERS, COLUMNID_PATH_ID);
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 50, TableColumn.INTERVAL_LIVE);
		column.addCellRefreshListener(new TableCellRefreshListener() {
			public void refresh(TableCell cell) {
				PeerImpl peer = (PeerImpl) cell.getDataSource();
				if (peer == null) {
					cell.setText("");
					return;
				}

				OverlayTransport tr = (OverlayTransport) peer.getPEPeer().getData(KEY_OVERLAY_TRANSPORT);
				if (tr == null) {
					cell.setText("");
					return;
				}
				Integer f = tr.getPathID();
				if (f == null) {
					cell.setText("");

					return;
				}
				if (!cell.setSortValue(f + ":") && cell.isValid()) {
					return;
				}

				cell.setText("" + f);
			}
		});
		tableManager.addColumn(column);

	}

	private void addConnectionWeightColumn(TableManager tableManager) {
		TableColumn column;

		column = tableManager.createColumn(TableManager.TABLE_TORRENT_PEERS, COLUMNID_CONNECTION_WEIGHT);
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 50, TableColumn.INTERVAL_LIVE);
		column.addCellRefreshListener(new TableCellRefreshListener() {
			public void refresh(TableCell cell) {
				PeerImpl peer = (PeerImpl) cell.getDataSource();
				if (peer == null) {
					cell.setText("");
					return;
				}
				if (!(peer.getPEPeer() instanceof PEPeerTransportProtocol)) {
					cell.setText("");
					return;
				}
				Double f = ((PEPeerTransportProtocol) peer.getPEPeer()).getWeight();

				if (!cell.setSortValue(f + ":") && cell.isValid()) {
					return;
				}
				cell.setText("" + f);
			}
		});
		tableManager.addColumn(column);
	}

	class FriendConnectionsView implements UnloadablePlugin {

		private static final String VIEWID = "Friend Connections";

		private ViewListener viewListener = null;

		private UISWTInstance swtInstance = null;

		public void initialize(PluginInterface pluginInterface) throws PluginException {

			// Get notified when the UI is attached
			pluginInterface.getUIManager().addUIListener(new UIManagerListener() {
				public void UIAttached(UIInstance instance) {
					if (instance instanceof UISWTInstance) {
						swtInstance = ((UISWTInstance) instance);
						// Do code here
						viewListener = new ViewListener();
						if (viewListener != null) {
							// Add it to the menu
							swtInstance.addView(UISWTInstance.VIEW_MAIN, VIEWID, viewListener);
							// Open it immediately
							swtInstance.openMainView(VIEWID, viewListener, null);
						}

					}
				}

				public void UIDetached(UIInstance instance) {
				}
			});
		}

		public void unload() throws PluginException {
		}
	}

	class ViewListener implements UISWTViewEventListener {

		UISWTView view = null;

		Table friendConnectionTable;

		Table overlayConnectionTable;

		FriendConnection selectedConnection = null;

		public boolean eventOccurred(UISWTViewEvent event) {
			switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				// System.out.println("TYPE_CREATE Called");
				/*
				 * We only want one view
				 * 
				 * If we wanted multiple views, we would need a class to handle
				 * one view. Then, we could set up a Map, with the key being the
				 * UISWTView, and the value being a new instance of the class.
				 * When the other types of events are called, we would lookup
				 * our class using getView(), and then pass the work to the our
				 * class.
				 */
				if (view != null)
					return false;
				view = event.getView();
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				// System.out.println("TYPE_INITIALIZE Called");
				initialize((Composite) event.getData());
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				// System.out.println("TYPE_DESTROY Called");
				view = null;
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				// System.out.println("TYPE_DATASOURCE_CHANGED Called");
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				// System.out.println("TYPE_FOCUSGAINED Called");
				break;

			case UISWTViewEvent.TYPE_FOCUSLOST:
				// System.out.println("TYPE_FOCUSLOST Called");
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				// System.out.println("TYPE_LANGUAGEUPDATE Called "
				// + Locale.getDefault().toString());
				break;
			}
			return true;
		}

		class columnHeader {
			String name;
			int width;

			public columnHeader(String name, int width) {
				this.name = name;
				this.width = width;
			}

		}

		private void initialize(Composite parent) {
			final Label lbl = new Label(parent, SWT.NONE);
			lbl.setText("Active friend connections");
			lbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			friendConnectionTable = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION);
			friendConnectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));
			friendConnectionTable.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
				}

				public void widgetSelected(SelectionEvent e) {
					TableItem i = friendConnectionTable.getItem(friendConnectionTable.getSelectionIndex());
					if (i != null) {
						selectedConnection = (FriendConnection) i.getData();
						refresh();
					}
				}
			});
			friendConnectionTable.addKeyListener(new KeyListener() {

				public void keyPressed(KeyEvent e) {
					if (e.keyCode == java.awt.event.KeyEvent.VK_SPACE || e.keyCode == java.awt.event.KeyEvent.VK_BACK_SPACE) {
						selectedConnection = null;
						refresh();
					}
				}

				public void keyReleased(KeyEvent e) {
					// TODO Auto-generated method stub

				}
			});
			for (columnHeader header : CONN_COLUMN_HEADERS) {
				org.eclipse.swt.widgets.TableColumn col = new org.eclipse.swt.widgets.TableColumn(friendConnectionTable, SWT.LEFT);
				col.setText(header.name);
				col.setWidth(header.width);
			}
			friendConnectionTable.setHeaderVisible(true);
			friendConnectionTable.setLinesVisible(true);

			final Label lbl2 = new Label(parent, SWT.NONE);
			lbl2.setText("Active overlay connections");
			lbl2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			overlayConnectionTable = new Table(parent, SWT.SINGLE | SWT.FULL_SELECTION);
			overlayConnectionTable.setLayoutData(new GridData(GridData.FILL_BOTH));

			for (columnHeader header : OVER_COLUMN_HEADERS) {
				org.eclipse.swt.widgets.TableColumn col = new org.eclipse.swt.widgets.TableColumn(overlayConnectionTable, SWT.LEFT);
				col.setText(header.name);
				col.setWidth(header.width);
			}
			overlayConnectionTable.setHeaderVisible(true);
			overlayConnectionTable.setLinesVisible(true);
		}

		private void refresh() {

			friendConnectionTable.setVisible(false);
			friendConnectionTable.removeAll();
			List<FriendConnection> conn = main.getOverlayManager().getFriendConnections();
			for (FriendConnection c : conn) {
				addConnections(c);
			}

			friendConnectionTable.setVisible(true);

			overlayConnectionTable.setVisible(false);
			overlayConnectionTable.removeAll();

			if (selectedConnection != null) {
				FriendConnection sel = selectedConnection;
				addOverlays(sel);
			} else {
				// add them all
				for (FriendConnection c : conn) {
					addOverlays(c);
				}
			}
			overlayConnectionTable.setVisible(true);

		}

		private final columnHeader[] CONN_COLUMN_HEADERS = { new columnHeader("connection id", 100), new columnHeader("remote friend", 100), new columnHeader("remote ip:port", 150), new columnHeader("upload speed", 150), new columnHeader("connection age", 100), new columnHeader("queue (ms)", 100), new columnHeader("queue (bytes)", 100), new columnHeader("proto uploaded", 100), new columnHeader("proto downloaded", 100), new columnHeader("data uploaded", 100), new columnHeader("data downloaded", 100), new columnHeader("forward num", 100), new columnHeader("transport num", 100), new columnHeader("last sent", 100), new columnHeader("last recv", 100), new columnHeader("avg upload", 100), new columnHeader("avg download", 100) };

		private void addConnections(FriendConnection c) {
			TableItem item = new TableItem(friendConnectionTable, SWT.NONE);
			item.setData(c);
			item.setText(new String[] { "" + c.hashCode(), c.getRemoteFriend().getNick(), c.getRemoteIp().getHostAddress() + ":" + c.getRemotePort(), formatter.formatByteCountToKiBEtcPerSec(c.getCurrentUploadSpeed()), formatter.formatTimeFromSeconds(c.getConnectionAge() / 1000), "not calculated", c.getForwardQueueLengthBytes() + "", formatter.formatByteCountToKiBEtc(c.getProtocolBytesUploaded()), formatter.formatByteCountToKiBEtc(c.getProtocolBytesDownloaded()), formatter.formatByteCountToKiBEtc(c.getDataBytesUploaded()), formatter.formatByteCountToKiBEtc(c.getDataBytesDownloaded()), "" + c.getOverlayForwards().size(), "" + c.getOverlayTransports().size(), formatter.formatTimeFromSeconds(c.getLastMessageSentTime() / 1000), formatter.formatTimeFromSeconds(c.getLastMessageRecvTime() / 1000), formatter.formatByteCountToKiBEtcPerSec((1000 * c.getDataBytesUploaded() + c.getProtocolBytesUploaded()) / c.getConnectionAge()),
					formatter.formatByteCountToKiBEtcPerSec((1000 * c.getDataBytesDownloaded() + c.getProtocolBytesDownloaded()) / c.getConnectionAge()) });

			if (c.equals(selectedConnection)) {
				friendConnectionTable.setSelection(item);
			}
		}

		private final columnHeader[] OVER_COLUMN_HEADERS = { new columnHeader("type", 100), new columnHeader("id", 100), new columnHeader("friend 1", 100), new columnHeader("f1 ip:port", 150), new columnHeader("friend 2", 100), new columnHeader("f2 ip:port", 150), new columnHeader("connection age", 100), new columnHeader("time since last msg", 150), new columnHeader("bytes in", 100), new columnHeader("bytes out", 100), new columnHeader("source message", 200) };

		private void addOverlays(FriendConnection sel) {
			Map<Integer, OverlayForward> forwards = sel.getOverlayForwards();
			for (Integer id : forwards.keySet()) {
				OverlayForward f = forwards.get(id);
				TableItem item = new TableItem(overlayConnectionTable, SWT.NONE);
				item.setText(new String[] { "forward", f.getChannelId() + "", sel.getRemoteFriend().getNick(), sel.getRemoteIp().getHostAddress() + ":" + sel.getRemotePort(), f.getRemoteFriend().getNick(), f.getRemoteIpPort(), formatter.formatTimeFromSeconds(f.getAge() / 1000), formatter.formatTimeFromSeconds(f.getLastMsgTime() / 1000), formatter.formatByteCountToKiBEtc(f.getBytesForwarded()), formatter.formatByteCountToKiBEtc(f.getBytesForwarded()), f.getSourceMessage().getDescription() });
			}

			Map<Integer, OverlayTransport> transports = sel.getOverlayTransports();
			for (Integer id : transports.keySet()) {
				OverlayTransport f = transports.get(id);
				TableItem item = new TableItem(overlayConnectionTable, SWT.NONE);
				item.setText(new String[] { "transport", f.getPathID() + "", "Me", "N/A", sel.getRemoteFriend().getNick(), sel.getRemoteIp().getHostAddress() + ":" + sel.getRemotePort(), formatter.formatTimeFromSeconds(f.getAge() / 1000), formatter.formatTimeFromSeconds(f.getLastMsgTime() / 1000), formatter.formatByteCountToKiBEtc(f.getBytesIn()), formatter.formatByteCountToKiBEtc(f.getBytesOut()) });
			}
		}
	}

}
