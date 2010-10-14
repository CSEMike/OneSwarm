package edu.washington.cs.oneswarm.ui.gwt.client.newui.settings;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.HelpButton;
import edu.washington.cs.oneswarm.ui.gwt.rpc.LocaleLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;

public class UISettingsPanel extends SettingsPanel {

	public static final String COOKIE_DISABLE_RIGHT_CLICK = "os-disable_right_click";
	public static final String COOKIE_DISABLE_DRAG_AND_DROP = "os-disable_drag_and_drop";

	final Label defaultActionLabel = new Label(msg.settings_interface_double_click());
	final Label languageLabel = new Label(msg.settings_interface_language());
	final ListBox defaultActionMenu = new ListBox();
	final ListBox languageMenu = new ListBox();

	final SettingsCheckBox autostart;
	final CheckBox show_menu_icon = new CheckBox(msg.settings_interface_show_in_menubar());
	final CheckBox right_click_menus = new CheckBox(msg.settings_interface_right_click());
	final CheckBox drag_and_drop = new CheckBox(msg.settings_interface_drag_and_drop());

	final TextBox maxSearchBox = new TextBox();

	boolean loadedAction = false, loadedMenu = false, loadedSearchLimit = false, loadedLanguage = false;
	private String oldLanguage = "";
	private boolean oldDragDropEnabled;
	private boolean oldRightClickEnabled;

	public UISettingsPanel() {
		super();

		defaultActionMenu.setVisibleItemCount(1);
		defaultActionMenu.addItem("...");

		OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "OneSwarm.ui.double.click", new AsyncCallback<Integer>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Integer result) {
				defaultActionMenu.clear();

				if (result < 0 || result > 2) {
					System.err.println("double click action out of bounds: " + result);
					result = 0;
				}

				defaultActionMenu.addItem(msg.settings_interface_double_click_browser());
				defaultActionMenu.addItem(msg.settings_interface_double_click_default_player());
				defaultActionMenu.addItem(msg.settings_interface_double_click_open_dir());

				defaultActionMenu.setSelectedIndex(result);

				loadedAction = true;
				if (loadedAction && loadedMenu) {
					loadNotify();
				}
			}
		});

		languageMenu.setVisibleItemCount(1);
		languageMenu.addItem("...");
		updateLanguageBox();

		HorizontalPanel hp = new HorizontalPanel();
		hp.add(defaultActionLabel);
		hp.add(defaultActionMenu);
		hp.setCellVerticalAlignment(defaultActionLabel, ALIGN_MIDDLE);
		hp.setCellVerticalAlignment(defaultActionMenu, ALIGN_MIDDLE);

		hp.setSpacing(3);
		this.add(hp);

		HorizontalPanel languagePanel = new HorizontalPanel();
		languagePanel.add(languageLabel);
		languagePanel.add(languageMenu);
		languagePanel.setCellVerticalAlignment(languageLabel, ALIGN_MIDDLE);
		languagePanel.setCellVerticalAlignment(languageMenu, ALIGN_MIDDLE);
		this.add(languagePanel);

		OneSwarmRPCClient.getService().getPlatform(OneSwarmRPCClient.getSessionID(), new AsyncCallback<String>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(String result) {
				if (result.equals("osx")) {
					UISettingsPanel.this.add(show_menu_icon);
				}
			}
		});

		OneSwarmRPCClient.getService().getBooleanParameterValue(OneSwarmRPCClient.getSessionID(), "Enable System Tray", new AsyncCallback<Boolean>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Boolean result) {
				show_menu_icon.setValue(result);

				loadedMenu = true;
				if (loadedAction && loadedMenu) {
					loadNotify();
				}
			}
		});

		HorizontalPanel limitSearchPanel = new HorizontalPanel();
		limitSearchPanel.setSpacing(4);
		Label l = new Label(msg.settings_interface_maxiumum_search_result());
		limitSearchPanel.add(l);
		limitSearchPanel.setCellVerticalAlignment(l, HorizontalPanel.ALIGN_MIDDLE);
		limitSearchPanel.add(maxSearchBox);
		limitSearchPanel.setCellVerticalAlignment(limitSearchPanel, HorizontalPanel.ALIGN_MIDDLE);
		final HelpButton h = new HelpButton(msg.settings_interface_maxiumum_search_result_help());
		limitSearchPanel.add(h);
		limitSearchPanel.setCellHorizontalAlignment(h, HorizontalPanel.ALIGN_RIGHT);
		limitSearchPanel.setCellVerticalAlignment(h, HorizontalPanel.ALIGN_MIDDLE);
		this.add(limitSearchPanel);
		maxSearchBox.setEnabled(false);
		maxSearchBox.setEnabled(false);
		if (Cookies.getCookie(COOKIE_DISABLE_RIGHT_CLICK) == null) {
			Cookies.setCookie(COOKIE_DISABLE_RIGHT_CLICK, "1", OneSwarmConstants.TEN_YEARS_FROM_NOW);
		}
		if (!OneSwarmGWT.isRemoteAccess() && OneSwarmGWT.isWindows()) {
			autostart = new SettingsCheckBox(msg.settings_interface_start_with_windows(), "autostart");
			this.add(autostart);
		} else {
			autostart = null;
		}

		oldRightClickEnabled = "0".equals(Cookies.getCookie(COOKIE_DISABLE_RIGHT_CLICK));
		right_click_menus.setValue(oldRightClickEnabled);
		this.add(right_click_menus);

		if (Cookies.getCookie(COOKIE_DISABLE_DRAG_AND_DROP) == null) {
			Cookies.setCookie(COOKIE_DISABLE_DRAG_AND_DROP, "1", OneSwarmConstants.TEN_YEARS_FROM_NOW);
		}
		oldDragDropEnabled = "0".equals(Cookies.getCookie(COOKIE_DISABLE_DRAG_AND_DROP));
		drag_and_drop.setValue(oldDragDropEnabled);
		this.add(drag_and_drop);

		maxSearchBox.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				char keyCode = event.getCharCode();
				if ((!Character.isDigit(keyCode)) && (keyCode != (char) KeyCodes.KEY_TAB) && (keyCode != (char) KeyCodes.KEY_BACKSPACE) && (keyCode != (char) KeyCodes.KEY_DELETE) && (keyCode != (char) KeyCodes.KEY_ENTER) && (keyCode != (char) KeyCodes.KEY_HOME) && (keyCode != (char) KeyCodes.KEY_END) && (keyCode != (char) KeyCodes.KEY_LEFT) && (keyCode != (char) KeyCodes.KEY_UP) && (keyCode != (char) KeyCodes.KEY_RIGHT) && (keyCode != (char) KeyCodes.KEY_DOWN)) {
					// TextBox.cancelKey() suppresses the current keyboard
					// event.
					maxSearchBox.cancelKey();
				}
			}
		});

		OneSwarmRPCClient.getService().getIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "oneswarm.max.ui.search.results", new AsyncCallback<Integer>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(Integer result) {
				maxSearchBox.setText(result.toString());
				maxSearchBox.setEnabled(true);
				loadedSearchLimit = true;
				loadNotify();
			}
		});
	}

	private void updateLanguageBox() {
		OneSwarmRPCClient.getService().getLocales(OneSwarmRPCClient.getSessionID(), new AsyncCallback<LocaleLite[]>() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}

			public void onSuccess(LocaleLite[] result) {
				languageMenu.clear();

				for (LocaleLite l : result) {
					languageMenu.addItem(l.getCountry() + " (" + l.getLanguage() + ")", l.getCode());
					loadedLanguage = true;
				}

				OneSwarmRPCClient.getService().getStringParameterValue(OneSwarmRPCClient.getSessionID(), "locale", new AsyncCallback<String>() {
					public void onFailure(Throwable caught) {
						// TODO Auto-generated method stub

					}

					public void onSuccess(String result) {
						int numItems = languageMenu.getItemCount();
						for (int i = 0; i < numItems; i++) {
							if (languageMenu.getValue(i).equals(result)) {
								languageMenu.setSelectedIndex(i);

								break;
							}
						}
						oldLanguage = languageMenu.getValue(languageMenu.getSelectedIndex());
					}
				});
			}
		});
	}

	public void sync() {
		boolean refreshRequired = false;

		System.out.println("sync UISettings");

		if (right_click_menus.getValue() != oldRightClickEnabled) {
			refreshRequired = true;
		}

		if (right_click_menus.getValue() == false) {
			Cookies.setCookie(COOKIE_DISABLE_RIGHT_CLICK, "1", OneSwarmConstants.TEN_YEARS_FROM_NOW);
		} else {
			Cookies.setCookie(COOKIE_DISABLE_RIGHT_CLICK, "0", OneSwarmConstants.TEN_YEARS_FROM_NOW);
		}

		if (drag_and_drop.getValue() != oldDragDropEnabled) {
			refreshRequired = true;
		}

		if (drag_and_drop.getValue() == false) {
			Cookies.setCookie(COOKIE_DISABLE_DRAG_AND_DROP, "1", OneSwarmConstants.TEN_YEARS_FROM_NOW);
		} else {
			Cookies.setCookie(COOKIE_DISABLE_DRAG_AND_DROP, "0", OneSwarmConstants.TEN_YEARS_FROM_NOW);
		}

		if (autostart != null) {
			autostart.save();
		}
		if (loadedAction) {
			OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "OneSwarm.ui.double.click", defaultActionMenu.getSelectedIndex(), new AsyncCallback<Void>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}

				public void onSuccess(Void result) {
					System.out.println("done");
				}
			});
		}

		if (loadedLanguage) {
			String newLanguage = languageMenu.getValue(languageMenu.getSelectedIndex());
			if (!newLanguage.equals(oldLanguage)) {
				refreshRequired = true;
				OneSwarmRPCClient.getService().setStringParameterValue(OneSwarmRPCClient.getSessionID(), "locale", newLanguage, new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}

					public void onSuccess(Void result) {
						updateLanguageBox();
					}
				});
			}
		}

		if (loadedMenu) {
			OneSwarmRPCClient.getService().setBooleanParameterValue(OneSwarmRPCClient.getSessionID(), "Enable System Tray", show_menu_icon.getValue(), new AsyncCallback<Void>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}

				public void onSuccess(Void result) {
					System.out.println("done");
				}
			});
		}

		if (loadedSearchLimit) {
			int max = 500;
			try {
				max = Integer.parseInt(maxSearchBox.getText());
			} catch (Exception e) {
				e.printStackTrace();
			}
			OneSwarmRPCClient.getService().setIntegerParameterValue(OneSwarmRPCClient.getSessionID(), "oneswarm.max.ui.search.results", max, new AsyncCallback<Void>() {
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}

				public void onSuccess(Void result) {
					System.out.println("done");
				}
			});
		}

		if (refreshRequired) {
			Timer t = new Timer() {
				public void run() {
					if (Window.confirm(msg.settings_interface_gui_refresh_required())) {
						reload();
					}
				}
			};
			// Schedule the timer to run once in 0.3 seconds.
			t.schedule(300);
		}

	}

	String validData() {
		// constrained input
		return null;
	}

	private native void reload() /*-{
		$wnd.location.reload();
	}-*/;

}
