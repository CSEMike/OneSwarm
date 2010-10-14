package edu.washington.cs.oneswarm.ui.gwt.client;

import java.util.Date;
import java.util.HashSet;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.ChatDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.EntireUIRoot;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.VideoDialog;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.creation.CreateSwarmDialogBox;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.FriendsDetailsListPanel;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportWizard;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.SettingsDialog;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class OneSwarmGWT implements EntryPoint {
	public static OSMessages msg = GWT.create(OSMessages.class);

	private static boolean showingDebugLog = false;

	private static HashSet<Updateable> componentList = new HashSet<Updateable>();

	private int count = 0;

	// final Label counter = new Label();

	// private TorrentListPanel torrentListPanel;

	// private final SidePanel sidePanel = new SidePanel();

	private static TextArea debugText = new TextArea();

	private static Timer uiUpdater;

	private static DialogBox errorDialog;

	private final DockPanel mainPanel = new DockPanel();

	private static boolean isConnectedThroughProxy = false;

	private static boolean useDebug = false;
	private static Boolean isIE = null;
	private static int ieVersion = -1;
	private static Boolean isWebkit = null;

	public static boolean hasDevUpdates() {
		return useDebug;
	}

	public static boolean isIE() {
		if (isIE == null) {
			updateBrowserInfo();
		}
		return isIE.booleanValue();
	}

	public static int getIEVersion() {
		if (ieVersion == -1) {
			updateBrowserInfo();
		}
		return ieVersion;
	}

	public static boolean isWebKit() {
		if (isWebkit == null) {
			updateBrowserInfo();
		}
		return isWebkit.booleanValue();
	}

	private static void updateBrowserInfo() {
		isIE = new Boolean(false);
		isWebkit = new Boolean(false);
		ieVersion = 0;
		String userAgent = getUserAgent();
		log("browser: " + userAgent);
		if (userAgent != null && userAgent.toLowerCase().contains("msie")) {
			isIE = new Boolean(true);
			if (parseIEVersion(userAgent) < 8) {
				ieVersion = 7;
			} else {
				ieVersion = 8;
			}
			log("detected browser: IE " + ieVersion);
		} else if (userAgent.toLowerCase().contains("webkit")) {
			isWebkit = new Boolean(true);

			log("detected browser: webkit");
		}
	}

	private static float parseIEVersion(String userAgent) {
		int offset = userAgent.toLowerCase().indexOf("msie ");
		if (offset == -1) {
			return 0;
		} else {
			return Float.parseFloat(userAgent.substring(offset + 5, userAgent.indexOf(";", offset)));
		}
	}

	// private static PickupDragController dragController = new
	// PickupDragController(
	// RootPanel.get(), true);

	/**
	 * This is the entry point method.
	 */

	public static native String getUserAgent() /*-{
		return navigator.userAgent;
	}-*/;

	public void onModuleLoad() {
		// for drag and drop
		// workaround for GWT issue 1813
		// http://code.google.com/p/google-web-toolkit/issues/detail?id=1813
		RootPanel.get().getElement().getStyle().setProperty("position", "relative");

		OneSwarmRPCClient.setSessionID(Cookies.getCookie("OneSwarm"));
		if (Cookies.getCookie("OneSwarmProxy") != null) {
			isConnectedThroughProxy = true;
		}

		/**
		 * Before we send the back end RPC (which blocks), we first set up a
		 * simple loading indicator
		 */
		final HorizontalPanel loadingPanel = new HorizontalPanel();
		loadingPanel.setWidth("100%");
		loadingPanel.setHeight("100%");
		final Label loadingLabel = new Label("Loading.");
		loadingPanel.add(loadingLabel);

		final Timer tick = (new Timer() {
			public void run() {
				if (loadingLabel.getText().equals("Loading..."))
					loadingLabel.setText("Loading.");
				else
					loadingLabel.setText(loadingLabel.getText() + ".");
			}
		});
		tick.scheduleRepeating(500);
		RootPanel.get().add(loadingPanel);

		/**
		 * Send RPC to start the back end on the servlet server
		 */
		OneSwarmRPCClient.getService().startBackend(new AsyncCallback<Boolean>() {
			public void onFailure(Throwable caught) {

				String message = caught.getClass().getName() + " / " + caught.toString() + " / " + caught.getMessage();
				if (caught instanceof StatusCodeException) {
					message += " status code: " + ((StatusCodeException) caught).getStatusCode();

					for (StackTraceElement e : caught.getStackTrace()) {
						message += e.getClassName() + ":" + e.getLineNumber() + " / " + e.getFileName() + " \n";
					}

					message += " msg: " + caught.getMessage();

					caught.printStackTrace();
				}

				new ReportableErrorDialogBox(message, true);
			}

			public void onSuccess(Boolean result) {
				useDebug = result;

				// No need to indicate loading anymore
				tick.cancel();
				RootPanel.get().remove(loadingPanel);

				// Start the proper GWT UI
				mainPanel.setWidth("100%");
				RootPanel.get().add(mainPanel);
				System.out.println("after backend start...");
				afterBackendStart();
			}
		});
	}

	public static boolean isRemoteAccess() {
		return isConnectedThroughProxy;
	}

	private void afterBackendStart() {

		System.out.println("in: afterBackendStart");

		debugText.setWidth("99%");
		debugText.setHeight("100px");
		
		debugText.setVisible(false);

		uiUpdater = new Timer() {
			public void run() {
				updateComponents();
			}
		};

		this.add(debugText, DockPanel.NORTH);

		EntireUIRoot root = new EntireUIRoot(useDebug);
		this.add(root, DockPanel.CENTER);
		this.add(root.getFooter(), DockPanel.SOUTH);

		startUpdates();
		
		Timer streamCode = new Timer() {
			public void run() {
				//if( isRemoteAccess() == false ) {
					loadSplitCode();
				//}
			}
		};
		// 1 second after startup, stream code for the rest of the UI 
		streamCode.schedule(1000);

		showDebug(showingDebugLog);

		History.fireCurrentHistoryState();
		
	}


	protected void loadSplitCode() {
		long start = System.currentTimeMillis();
		System.out.println("loading split code... ");
		
		SettingsDialog s = new SettingsDialog(null, null, -1);
		FriendsDetailsListPanel p = new FriendsDetailsListPanel(null);
		ChatDialog cd = new ChatDialog(null, null, null);
		FriendsImportWizard fw = new FriendsImportWizard("preload");
		CreateSwarmDialogBox cs = new CreateSwarmDialogBox(null);
		
		System.out.println("split code loaded: " + (System.currentTimeMillis()-start));
	}

	public static void showDebug(boolean show) {
		showingDebugLog = show;

		debugText.setVisible(showingDebugLog);
	}

	public static boolean isShowingDebug() {
		return showingDebugLog;
	}

	public OneSwarmGWT() {
		System.out.println("**********  oneswarm GWT, cwd");
	}

	private void add(Widget widget, DockPanel.DockLayoutConstant pos) {
		mainPanel.add(widget, pos);
		if (widget instanceof Updateable) {
			componentList.add((Updateable) widget);
		}
	}

	// private void add(Widget widget, String position) {
	// if (position == null) {
	// RootPanel.get().add(widget);
	// } else {
	// RootPanel.get(position).add(widget);
	// }
	// componentList.add(widget);
	// }

	private void updateComponents() {
		for (Updateable component : componentList) {
			component.update(count);
		}
		count++;
	}

	public static void addToUpdateTask(Updateable component) {
		componentList.add(component);
	}

	public static void removeFromUpdateTask(Updateable component) {
		componentList.remove(component);
	}

	public static void log(String message) {

		System.out.println(message);

		if (useDebug) {
			Date d = new Date();
			
			String prev = debugText.getText();
			if( prev.length() > 2048 ) { 
				prev = prev.substring(0, 2048);
			}
			
			debugText.setText(d.toString() + "\t" + message + "\n" + prev);
		}
	}

	// public static void cancelUpdates(String message) {
	// uiUpdater.cancel();
	//
	// corePinger.scheduleRepeating(1000);
	// if (errorDialog == null) {
	// errorDialog = new
	// ErrorDialog("It seems like the OneSwarm engine has stopped.\nError:\n" +
	// message);
	// }
	//
	// }

	private static void decreaseUpdateRate() {
		uiUpdater.cancel();
		uiUpdater.scheduleRepeating(30000);
	}

	private static void restoreUpdateRate() {
		uiUpdater.cancel();
		uiUpdater.scheduleRepeating(1000);
	}

	private static VideoDialog playerWindow = null;

	public static void registerPlayerWindow(VideoDialog d) {
		decreaseUpdateRate();
		// if we have one running, close it
		if (playerWindow != null) {
			playerWindow.onClick(null);
		}
		playerWindow = d;
	}

	public static void deRegisterPlayerWindow() {
		playerWindow = null;
		restoreUpdateRate();
	}

	private static FriendsImportWizard friendsImportWizard = null;

	public static void registerImportWizard(FriendsImportWizard w) {
		if (friendsImportWizard != null) {
			friendsImportWizard.hide();
		}
		friendsImportWizard = w;
	}

	private static void startUpdates() {
		if (errorDialog != null) {
			errorDialog.hide();
			errorDialog = null;
		}

		uiUpdater.scheduleRepeating(1000);
	}

	static class ErrorDialog extends DialogBox {
		// private GlassPanel panel;

		public ErrorDialog(String message) {
			super();
			super.setText("OneSwarm error");

			// panel = new GlassPanel(false);
			// panel.setWidth(Window.getClientWidth() + "px");
			// panel.setHeight(Window.getClientHeight() + "px");
			TextArea text = new TextArea();
			text.setText(message);
			text.setReadOnly(true);
			int width = (Window.getClientWidth() / 3);
			text.setWidth(width + "px");
			super.setWidget(text);
			show();
			super.setVisible(false);
			super.center();
			super.setStyleName("os-ErrorBox");
			super.setWidth(width + "px");
			super.setVisible(true);

		}

		public void show() {
			// log("running show");
			// RootPanel.get().add(panel, 0, 0);
			// Window.enableScrolling(false);
			super.show();
		}

		public void hide() {
			// panel.removeFromParent();
			// Window.enableScrolling(true);
			super.hide();
		}
	}

	public static boolean isWindows() {
		return getUserAgent().toLowerCase().contains("windows");
	}
}
