package org.gudy.azureus2.ui.swt.mainwindow;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;

/**
 * SWT Listener that, when fired, waits for AzureusCore to be available
 */
public abstract class ListenerNeedingCoreRunning
	implements Listener
{
	public final void handleEvent(final Event event) {
		CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				handleEvent(core, event);
			}
		});
	}

	public abstract void handleEvent(AzureusCore core, Event event);
}
