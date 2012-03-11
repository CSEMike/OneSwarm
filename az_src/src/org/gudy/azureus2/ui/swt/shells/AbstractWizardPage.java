package org.gudy.azureus2.ui.swt.shells;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;

abstract public class AbstractWizardPage
	implements IWizardPage
{
	public static final String BUTTON_OK = "button.ok";

	public static final String BUTTON_CANCEL = "button.cancel";

	public static final String BUTTON_NEXT = "button.next";

	public static final String BUTTON_BACK = "button.back";

	private MultipageWizard wizard;

	private Composite pageControl;

	private Composite contentPanel;

	private Composite toolbarPanel;

	/**
	 * A map of buttonID(String)/<code>Button</code>; using LinkedHashMap since the order the buttons are added is important
	 */
	private Map buttons = new LinkedHashMap();

	protected SelectionListener defaultButtonListener;

	/**
	 * A little extra margin so the buttons are a little wider; typically the native buttons
	 * are just a little wider than the text but a slightly wider button looks nicer
	 */
	private int buttonExtraMargin = 50;

	public AbstractWizardPage(MultipageWizard wizard) {
		this.wizard = wizard;
	}

	/**
	 * Returns the main Composite where subclasses can create controls
	 */
	public Composite createControls(Composite parent) {
		pageControl = new Composite(parent, SWT.NONE);
		pageControl.setBackground(Colors.red);

		GridLayout gLayout = new GridLayout();
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		gLayout.verticalSpacing = 0;
		pageControl.setLayout(gLayout);

		contentPanel = new Composite(pageControl, SWT.NONE);
		contentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label separator2 = new Label(pageControl, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		toolbarPanel = new Composite(pageControl, SWT.NONE);
		toolbarPanel.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		GridLayout gLayout2 = new GridLayout(3, false);
		gLayout2.marginHeight = 16;
		gLayout2.marginWidth = 16;
		toolbarPanel.setLayout(gLayout2);

		defaultButtonListener = new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (true == BUTTON_OK.equals(e.widget.getData("button.id"))) {
					performOK();
				} else if (true == BUTTON_CANCEL.equals(e.widget.getData("button.id"))) {
					performCancel();
				} else if (true == BUTTON_NEXT.equals(e.widget.getData("button.id"))) {
					performNext();
				} else if (true == BUTTON_BACK.equals(e.widget.getData("button.id"))) {
					performBack();
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		};

		/*
		 * This invisible label is used to ensure the buttons are flushed-right
		 */
		Label dummy = new Label(toolbarPanel, SWT.NONE);
		dummy.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		createButtons(toolbarPanel);

		return contentPanel;
	}

	public void fullScreen(boolean isFullScreen) {
		toolbarPanel.setVisible(false == isFullScreen);
		((GridData) toolbarPanel.getLayoutData()).exclude = isFullScreen;
		pageControl.layout(true, true);

		getWizard().fullScreen(isFullScreen);

	}

	/**
	 * Default buttons include Cancel, OK, Next, and Back
	 * Subclasses may override to add more buttons or create a custom set of buttons
	 *  
	 * @param buttonPanel
	 */
	protected void createButtons(Composite buttonPanel) {

		createButton(BUTTON_CANCEL, MessageText.getString("Button.cancel"),
				defaultButtonListener);
		createButton(BUTTON_BACK, MessageText.getString("wizard.previous"),
				defaultButtonListener);
		createButton(BUTTON_NEXT, MessageText.getString("wizard.next"),
				defaultButtonListener);
		createButton(BUTTON_OK, MessageText.getString("wizard.finish"),
				defaultButtonListener);

	}

	protected Button createButton(String buttonID, String buttonText,
			SelectionListener listener) {
		if (null == buttonID) {
			throw new IllegalArgumentException("A button requires a non-null ID");
		}

		if (true == buttons.containsKey(buttonID)) {
			Debug.out("AbstractWizardPage:: a button with this same ID already exists ID:"
					+ buttonID);
			return (Button) buttons.get(buttonID);
		}

		Button button = new Button(toolbarPanel, SWT.PUSH);
		GridData gData = new GridData(SWT.END, SWT.BOTTOM, false, false);
		gData.widthHint = button.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
				+ buttonExtraMargin;
		button.setLayoutData(gData);

		/*
		 * Add listener if given; for default buttons this is used in place of the default listener 
		 */
		if (null != listener) {
			button.addSelectionListener(listener);
		}

		button.setText(buttonText);
		button.setData("button.id", buttonID);

		buttons.put(buttonID, button);

		adjustToolbar();

		return button;
	}

	/**
	 * Enable/Disable the button with the given id
	 * @param buttonID
	 * @param value
	 */
	protected void enableButton(String buttonID, boolean value) {
		if (false == buttons.containsKey(buttonID)) {
			Debug.out("AbstractWizardPage:: a button with this ID is not found ID:"
					+ buttonID);
			return;
		}

		((Button) buttons.get(buttonID)).setEnabled(value);

		toolbarPanel.layout(true);
	}

	/**
	 * Show or hide the button with the given id
	 * @param buttonID
	 * @param value
	 */
	protected void showButton(String buttonID, boolean value) {
		if (false == buttons.containsKey(buttonID)) {
			Debug.out("AbstractWizardPage:: a button with this ID is not found ID:"
					+ buttonID);
			return;
		}

		Button button = (Button) buttons.get(buttonID);
		button.setVisible(value);

		if (true == value) {
			GridData gData = ((GridData) button.getLayoutData());
			gData.exclude = false;
			gData.widthHint = button.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
					+ buttonExtraMargin;
		} else {
			GridData gData = ((GridData) button.getLayoutData());
			gData.exclude = true;
			gData.widthHint = 0;
		}

		toolbarPanel.layout(true);
	}

	/**
	 * Return the <code>Button</code> with the given id; returns <code>null</code> if button is not found
	 * @param buttonID
	 * @return
	 */
	protected Button getButton(String buttonID) {
		if (false == buttons.containsKey(buttonID)) {
			return null;
		}

		return (Button) buttons.get(buttonID);
	}

	/**
	 * called when the default OK button is pressed
	 */
	public void performOK() {
		//Does nothing
	}

	/**
	 * Called when the default Cancel button is pressed
	 */
	public void performCancel() {
		getWizard().performCancel();
	}

	/**
	 * Called when the default Next button is pressed
	 */
	public void performNext() {
		getWizard().performNext();
	}

	/**
	 * Called when the default Back button is pressed
	 */
	public void performBack() {
		getWizard().performBack();
	}

	/**
	 * Adjusting the number of columns to correspond with the number of buttons
	 */
	private void adjustToolbar() {
		/*
		 * NOTE: we're adding 1 to the number of columns because there is always an invisible
		 * label on the far left used for spacing so the buttons are right-aligned properly
		 */
		((GridLayout) toolbarPanel.getLayout()).numColumns = buttons.size() + 1;
		toolbarPanel.layout(true);
	}

	public Control getControl() {
		return pageControl;
	}

	public MultipageWizard getWizard() {
		return wizard;
	}

	public String getDesciption() {
		return null;
	}

	public String getTitle() {
		return null;
	}

	public String getWindowTitle() {
		return null;
	}

	public boolean isComplete() {
		return false;
	}

	public void performDispose() {
	}

	public void performFinish() {
	}

	public boolean setComplete() {
		return true;
	}

	public void performAboutToBeHidden() {
	}

	public void performAboutToBeShown() {
		/*
		 * If it's the last page then disable the 'Next' button;
		 * if there is no 'Next' button this will do nothing
		 */
		if (null != getButton(BUTTON_NEXT)) {
			enableButton(BUTTON_NEXT, false == getWizard().isLastPage(getPageID()));
		}
		/*
		 * If it's the first page then disable the 'Back' button;
		 * if there is no 'Back' button this will do nothing
		 */
		if (null != getButton(BUTTON_BACK)) {
			enableButton(BUTTON_BACK, false == getWizard().isFirstPage(getPageID()));
		}
	}

	public boolean isInitOnStartup(){
		return false;
	}
}
