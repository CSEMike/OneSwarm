package edu.washington.cs.oneswarm.ui.gwt.client.newui;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

public class NavigationFilterBar extends HorizontalPanel implements KeyUpHandler {
	private static OSMessages msg = OneSwarmGWT.msg;

	private static final String CSS_SEARCH_FIELD = "os-searchField";
	public static final int MIN_SEARCH_LENGTH = 3;
	private TextBox searchField = null;

	final NavigationFilterBar this_shadow = this;

	boolean timerScheduled = false;
	final Timer refreshTimer = new Timer() {
		String lastRefreshText = "";

		public void run() {
			// toLowerCase() since we aren't going to be doing case sensitive
			// comparisons
			if (lastRefreshText.equals(searchField.getText()) == false) {
				EntireUIRoot.getRoot(this_shadow).filterTextChanged(searchField.getText().toLowerCase());
				lastRefreshText = searchField.getText();
			}
			timerScheduled = false;
			searchField.setFocus(true);
		}
	};
	private long nextEnterThresh = 0;
	private final boolean focusOnLoad;

	public NavigationFilterBar(boolean focusOnLoad) {
		addStyleName("os-search_bar");
		this.focusOnLoad = focusOnLoad;
		Label filterLabel = new Label(msg.top_search());

		this.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
		filterLabel.addStyleName("os-searchLabel");

		add(filterLabel);
		searchField = new TextBox();
		searchField.addStyleName(CSS_SEARCH_FIELD);
		
		DOM.setStyleAttribute(searchField.getElement(), "color", "grey");
		searchField.setText(msg.top_f2f_search());
		
		searchField.addFocusListener(new FocusListener(){
			public void onFocus(Widget sender) {
				if( searchField.getText().equals(msg.top_f2f_search()) ) {
					searchField.setText("");
				}
				DOM.setStyleAttribute(searchField.getElement(), "color", "black");
			}

			public void onLostFocus(Widget sender) {
				if( searchField.getText().length() == 0 ) {
					DOM.setStyleAttribute(searchField.getElement(), "color", "grey");
					searchField.setText(msg.top_f2f_search());
				}
			}});
		
		add(searchField);

		searchField.setFocus(true);

		searchField.addKeyUpHandler(this);

		// Button searchButton = new Button("Search Friend Network");
		// searchButton.addClickListener(new ClickListener() {
		// public void onClick(Widget sender) {
		// doSearch();
		// }
		// });
		// add(searchButton);
		// setSpacing(5);
	}

	public String getSearchFieldText() {
		return searchField.getText();
	}

	public void clearSearchFieldText() {
		searchField.setText("");
		if (timerScheduled == false) {
			timerScheduled = true;
			refreshTimer.schedule(1);
		}
	}

	private void doSearch() {
		if (searchField.getText().trim().length() < MIN_SEARCH_LENGTH) {
			Window.alert("Search string must be at least " + MIN_SEARCH_LENGTH + " characters in length");
			/**
			 * This to keep from cycling through an endless series of 'too
			 * short' dialogs (since hitting return on one will trigger the
			 * listener again)
			 */
			nextEnterThresh = System.currentTimeMillis() + 1000;
			return;
		}

		EntireUIRoot.getRoot(this_shadow).displaySearch(searchField.getText());
	}

	public void focusSearch() {
		searchField.setFocus(true);
	}

	public void onLoad() {
		if (focusOnLoad) {
			searchField.setFocus(true);
		}
	}

	public void onKeyUp(KeyUpEvent event) {
		int keyCode = event.getNativeKeyCode();
		/**
		 * Doing this after every keystroke could result in lag if there are
		 * many swarms to filter. Instead, we impose a 100 ms delay after each
		 * key press to allow more keystrokes before we filter (but still
		 * perform frequently enough to avoid apparent filtering lag)
		 */
		if (timerScheduled == false) {
			timerScheduled = true;
			refreshTimer.schedule(100);
		}

		if (keyCode == KeyCodes.KEY_ESCAPE) {
			searchField.cancelKey();
			searchField.setText("");
			if (timerScheduled == false) {
				timerScheduled = true;
				refreshTimer.schedule(1);
			}
		} else if (keyCode == KeyCodes.KEY_ENTER && nextEnterThresh < System.currentTimeMillis()) {
			doSearch();
		}
	}
}
