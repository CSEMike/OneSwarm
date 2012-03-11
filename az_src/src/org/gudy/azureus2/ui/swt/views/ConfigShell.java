package org.gudy.azureus2.ui.swt.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * A shell containing the <code>ConfigView</code>
 * This is used to pop-up the configs in a Shell as opposed to hosting it in the application
 * This class is used to ensure that only one shell is opened at any time.
 * @author khai
 *
 */
public class ConfigShell
{

	private static ConfigShell instance;

	private Shell shell;

	private ConfigView configView;

	private UISWTViewImpl swtView;

	public static ConfigShell getInstance() {
		if (null == instance) {
			instance = new ConfigShell();
		}
		return instance;
	}

	private ConfigShell() {
	}

	/**
	 * Opens the <code>ConfigView</code> inside a pop-up <code>Shell</code>.
	 * If the Shell is opened already then just force it active
	 * @param width
	 * @param height
	 */
	public void open(String section) {
		if (null != shell && false == shell.isDisposed()) {
			configView.selectSection(section);
			if (true == shell.getMinimized()) {
				shell.setMinimized(false);
			}
			shell.forceActive();
			shell.forceFocus();
		} else {
			shell = ShellFactory.createMainShell(SWT.SHELL_TRIM & ~SWT.MIN);
			shell.setLayout(new GridLayout());
			shell.setText(MessageText.getString(MessageText.resolveLocalizationKey("ConfigView.title.full")));
			Utils.setShellIcon(shell);
			configView = new ConfigView();
			try {
				swtView = new UISWTViewImpl(null, "ConfigView", configView, section);
			} catch (Exception e1) {
				Debug.out(e1);
			}
			swtView.initialize(shell);
			configView.selectSection(section);

			/*
			 * Set default size and centers the shell if it's configuration does not exist yet
			 */
			if (null == COConfigurationManager.getStringParameter(
					"options.rectangle", null)) {
				Rectangle shellBounds = shell.getMonitor().getBounds();
				Point size = new Point(shellBounds.width * 10 / 11,
						shellBounds.height * 10 / 11);
				if (size.x > 1400) {
					size.x = 1400;
				}
				if (size.y > 700) {
					size.y = 700;
				}
				shell.setSize(size);
				Utils.centerWindowRelativeTo(shell, getMainShell());
			}

			Utils.linkShellMetricsToConfig(shell, "options");

			/*
			 * Auto-save when the shell closes
			 */
			shell.addListener(SWT.Close, new Listener() {
				public void handleEvent(Event event) {
					configView.save();
					event.doit = true;
				}
			});
			
			shell.addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					if (e.detail == SWT.TRAVERSE_ESCAPE) {
						shell.dispose();
					}
				}
			});

			shell.addDisposeListener(
				new DisposeListener()
				{
					public void 
					widgetDisposed(
						DisposeEvent arg0 ) 
					{
						close();
					}
				});
			
			shell.open();
		}
	}

	private void 
	close() 
	{
		// if (null != shell && false == shell.isDisposed()) {
		//	shell.close();
		// }
			// clear these down as view now dead
		
		if (swtView != null) {
  		swtView.triggerEvent(UISWTViewEvent.TYPE_DESTROY, null);
  		swtView = null;
		}
		
		shell		= null;
		configView	= null;
	}

	private Shell getMainShell() {
		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (null != uiFunctions) {
			return uiFunctions.getMainShell();
		}

		throw new IllegalStateException(
				"No instance of UIFunctionsSWT found; the UIFunctionsManager might not have been initialized properly");

	}
}
