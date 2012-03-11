package org.gudy.azureus2.ui.swt.mainwindow;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.DisplayFormatters;

import com.aelitis.azureus.plugins.startstoprules.defaultplugin.StartStopRulesDefaultPlugin;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * A convenience class for creating the Debug menu
 * <p>
 * This has been extracted out into its own class since it does not really belong to production code
 * @author knguyen
 *
 */
public class DebugMenuHelper
{
	/**
	 * Creates the Debug menu and its children
	 * NOTE: This is a development only menu and so it's not modularized into separate menu items
	 * because this menu is always rendered in its entirety
	 * @param menu
	 * @param mainWindow
	 * @return
	 */
	public static Menu createDebugMenuItem(final Menu menu) {
		MenuItem item;

		final UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (null == uiFunctions) {
			throw new IllegalStateException(
					"UIFunctionsManagerSWT.getUIFunctionsSWT() is returning null");
		}

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("&Debug");
		Menu menuDebug = new Menu(menu.getParent(), SWT.DROP_DOWN);
		item.setMenu(menuDebug);

		item = new MenuItem(menuDebug, SWT.CASCADE);
		item.setText("ScreenSize");
		Menu menuSS = new Menu(menu.getParent(), SWT.DROP_DOWN);
		item.setMenu(menuSS);

		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("640x400");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(640, 400);
			}
		});

		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("800x560");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(850, 560);
			}
		});

		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1024x700");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(1024, 700);
			}
		});

		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1024x768");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(1024, 768);
			}
		});
		
		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1152x784");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(1152, 784);
			}
		});

		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1280x720");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(1280, 720);
			}
		});

		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1280x1024");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(1280, 1024);
			}
		});
		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1440x820");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(1440, 820);
			}
		});

		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1600x970");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(1600, 970);
			}
		});

		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1920x1200");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(1920, 1200);
			}
		});
		
		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("2560x1520");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				uiFunctions.getMainShell().setSize(2560, 1520);
			}
		});

		item = new MenuItem(menuDebug, SWT.NONE);
		item.setText("Reload messagebundle");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MessageText.loadBundle(true);
				DisplayFormatters.setUnits();
				DisplayFormatters.loadMessages();
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshLanguage();
				}
			}
		});
		
		item = new MenuItem(menuDebug, SWT.CHECK);
		item.setText("SR ChangeFlagChecker Paused");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				StartStopRulesDefaultPlugin.pauseChangeFlagChecker = !StartStopRulesDefaultPlugin.pauseChangeFlagChecker;
				((MenuItem)e.widget).setSelection(StartStopRulesDefaultPlugin.pauseChangeFlagChecker);
			}
		});
		
		

		return menuDebug;
	}
}
