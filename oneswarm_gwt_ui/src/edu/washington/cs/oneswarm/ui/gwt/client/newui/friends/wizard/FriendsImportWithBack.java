package edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard;

import java.util.LinkedList;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;

public abstract class FriendsImportWithBack extends VerticalPanel {
	protected static OSMessages msg = OneSwarmGWT.msg;
	protected final static int WIDTH = FriendsImportWizard.WIDTH - 2;
	protected final static int TEXT_AREA_WIDTH = FriendsImportWizard.WIDTH - 10;

	private FriendsImportWithBackStep currentPanel;
	private VerticalPanel stepPanel = new VerticalPanel();

	private LinkedList<FriendsImportWithBackStep> panels = new LinkedList<FriendsImportWithBackStep>();
	protected final Button nextButton = new Button("Next");
	protected final Button backButton = new Button("Back");

	public FriendsImportWithBack() {
		this.add(stepPanel);
		/*
		 * buttons
		 */
		HorizontalPanel buttonPanel = new HorizontalPanel();
		buttonPanel.setSpacing(5);

		backButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (!back()) {
					onLastBack();
				}
			}
		});
		buttonPanel.add(backButton);

		nextButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				FriendsImportWithBackStep nextPanel = currentPanel.createNextPanel();
				if (nextPanel != null) {
					next(nextPanel);
				} else {
					onLastNext();
				}
			}
		});
		buttonPanel.add(nextButton);
		this.add(buttonPanel);
		this.setCellHorizontalAlignment(buttonPanel, ALIGN_RIGHT);
	}

	protected boolean back() {
		// first, remove the current panel from the list
		if (panels.size() > 1) {
			panels.removeLast();

			stepPanel.remove(currentPanel);
			currentPanel = panels.getLast();
			stepPanel.add(currentPanel);
			updateButtonText();
			return true;
		}

		return false;
	}

	public void setFirstStep(FriendsImportWithBackStep firstStep) {
		next(firstStep);
	}

	private void next(FriendsImportWithBackStep nextPanel) {
		panels.add(nextPanel);
		if (currentPanel != null) {
			stepPanel.remove(currentPanel);
		}
		currentPanel = nextPanel;
		stepPanel.add(currentPanel);
		updateButtonText();
	}

	private void updateButtonText() {
		String text = currentPanel.getNextButtonText();
		if (text == null) {
			text = "Next";
		}
		nextButton.setText(text);
	}

	protected abstract void onLastBack();

	protected abstract void onLastNext();

	public static abstract class FriendsImportWithBackStep extends VerticalPanel {
		public abstract FriendsImportWithBackStep createNextPanel();

		public abstract String getNextButtonText();
	}
}
