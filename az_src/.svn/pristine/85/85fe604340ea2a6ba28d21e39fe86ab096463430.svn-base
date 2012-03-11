package org.gudy.azureus2.ui.swt.shells;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.ui.swt.utils.FontUtils;

abstract public class MultipageWizard
{

	private Shell shell;

	private int shellStyle;

	private Composite topPanel;

	private Composite contentPanel;

	private Label titleLabel;

	private Label descriptionLabel;

	/**
	 * A map of pageID(String)/<code>IWizardPage</code>; using LinkedHashMap since the order the pages are inserted is important
	 */
	private Map pages = new LinkedHashMap();

	private StackLayout contentStackLayout;

	private IWizardPage currentPage;

	private IWizardPage previousPage;

	private List initializedPages = new ArrayList();

	public abstract void createPages();

	private void init() {

		shell = ShellFactory.createMainShell(shellStyle);

		createControls();
		createPages();

	}

	private void createControls() {
		GridLayout gLayout = new GridLayout();
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		gLayout.verticalSpacing = 0;
		shell.setLayout(gLayout);
		Utils.setShellIcon(shell);

		topPanel = new Composite(shell, SWT.NONE);
		topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout gLayout1 = new GridLayout();
		gLayout1.marginBottom = 10;
		topPanel.setLayout(gLayout1);
		topPanel.setBackground(shell.getDisplay().getSystemColor(
				SWT.COLOR_LIST_BACKGROUND));
		topPanel.setBackgroundMode(SWT.INHERIT_FORCE);

		Label separator1 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		contentPanel = new Composite(shell, SWT.NONE);
		contentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		contentStackLayout = new StackLayout();
		contentPanel.setLayout(contentStackLayout);

		titleLabel = new Label(topPanel, SWT.NONE);
		titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		FontUtils.setFontHeight(titleLabel, 16, SWT.NORMAL);

		descriptionLabel = new Label(topPanel, SWT.WRAP);
		GridData gData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gData.horizontalIndent = 10;
		descriptionLabel.setLayoutData(gData);

		shell.layout(true, true);

	}

	public void fullScreen(boolean isFullScreen) {
		topPanel.setVisible(false == isFullScreen);
		((GridData) topPanel.getLayoutData()).exclude = isFullScreen;
		shell.layout(true, true);
	}

	public boolean addPage(IWizardPage page) {
		if (null == page) {
			return false;
		}
		if (true == pages.containsKey(page.getPageID())) {
			Debug.out("MultipageWizard:: a page with this ID already exists ID:"
					+ page.getPageID());
			return false;
		}

		pages.put(page.getPageID(), page);

		if (true == page.isInitOnStartup()) {
			page.createControls(contentPanel);
			initializedPages.add(page.getPageID());
		}
		return true;
	}

	public boolean isFirstPage(String pageID) {
		if (false == pages.isEmpty()) {
			return pageID.equals(((IWizardPage) pages.values().iterator().next()).getPageID());
		}
		return false;
	}

	public boolean isLastPage(String pageID) {
		if (false == pages.isEmpty()) {
			IWizardPage page = null;
			for (Iterator iterator = pages.values().iterator(); iterator.hasNext();) {
				page = (IWizardPage) iterator.next();
			}

			if (null != page) {
				return page.getPageID().equals(pageID);
			}
		}
		return false;
	}

	public boolean removePage(IWizardPage page) {
		if (null == page) {
			return false;
		}

		if (false == pages.containsKey(page.getPageID())) {
			Debug.out("MultipageWizard:: a page with this ID is not found ID:"
					+ page.getPageID());
			return false;
		}
		pages.remove(page.getPageID());
		page.performDispose();
		return true;
	}

	public void showPage(String pageID) {
		if (false == pages.containsKey(pageID)) {
			Debug.out("MultipageWizard:: a page with this ID is not found ID:"
					+ pageID);
			return;
		}

		IWizardPage page = (IWizardPage) pages.get(pageID);

		if (null != currentPage) {
			if (true == currentPage.getPageID().equals(page.getPageID())) {
				return;
			}
			currentPage.performAboutToBeHidden();
		}

		/*
		 * Initializing the page if not done already
		 */
		if (false == initializedPages.contains(page.getPageID())) {
			page.createControls(contentPanel);
			initializedPages.add(page.getPageID());
		}

		page.performAboutToBeShown();

		previousPage = currentPage;
		currentPage = page;
		contentStackLayout.topControl = page.getControl();

		update();

		contentPanel.layout(true);
	}

	public void open() {
		/*
		 * Show the first page
		 */
		if (false == pages.isEmpty()) {
			IWizardPage page = (IWizardPage) pages.values().iterator().next();
			showPage(page.getPageID());
		}

		shell.open();
	}

	private void update() {
		if (null != currentPage) {
			setText(currentPage.getWindowTitle());
			setTitle(currentPage.getTitle());
			setDescription(currentPage.getDesciption());
		}
	}

	public void setTitle(String title) {
		titleLabel.setText(title + "");
	}

	public void setDescription(String description) {
		descriptionLabel.setText(description + "");
	}

	/**
	 * Return the <code>IWizardPage</code> with the given id; returns <code>null</code> if page is not found
	 * @param pageID
	 * @return
	 */
	public IWizardPage getPage(String pageID) {
		if (false == pages.containsKey(pageID)) {
			Debug.out("MultipageWizard:: a Page with this ID is not found ID:"
					+ pageID);
			return null;
		}

		return (IWizardPage) pages.get(pageID);
	}

	public void performCancel() {
		close();
	}

	public void performNext() {
		if (true == pages.isEmpty()) {
			return;
		}

		if (null == currentPage) {
			IWizardPage page = (IWizardPage) pages.values().iterator().next();
			showPage(page.getPageID());
		} else {
			boolean foundCurrent = false;
			for (Iterator iterator = pages.values().iterator(); iterator.hasNext();) {
				IWizardPage page = (IWizardPage) iterator.next();
				if (true == foundCurrent) {
					showPage(page.getPageID());
					return;
				}

				if (page.getPageID().equals(currentPage.getPageID())) {
					foundCurrent = true;
				}
			}

			if (false == foundCurrent) {
				Debug.out("MultipageWizard:: there is no more page to go to");
			}

		}
	}

	public void performBack() {

		if (null != previousPage) {
			showPage(previousPage.getPageID());
		}
	}

	/* ===========================================
		 * Below are just some convenience delegations
		 * =========================================== */
	public Shell getShell() {
		return shell;
	}

	public void close() {
		shell.close();
	}

	public Object getData(String key) {
		return shell.getData(key);
	}

	public Point getLocation() {
		return shell.getLocation();
	}

	public Point getSize() {
		return shell.getSize();
	}

	public String getText() {
		return shell.getText();
	}

	public String getToolTipText() {
		return shell.getToolTipText();
	}

	public void setBounds(int x, int y, int width, int height) {
		shell.setBounds(x, y, width, height);
	}

	public void setData(String key, Object value) {
		shell.setData(key, value);
	}

	public void setLocation(int x, int y) {
		shell.setLocation(x, y);
	}

	public void setSize(int width, int height) {
		shell.setSize(width, height);
	}

	public void setText(String string) {
		shell.setText(string);
	}

	public void setToolTipText(String string) {
		shell.setToolTipText(string);
	}

	public void setVisible(boolean visible) {
		shell.setVisible(visible);
	}

	public Image getImage() {
		return shell.getImage();
	}

	public void setImage(Image image) {
		shell.setImage(image);
	}

}
