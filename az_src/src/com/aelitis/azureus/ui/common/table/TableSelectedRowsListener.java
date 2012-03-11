package com.aelitis.azureus.ui.common.table;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.ui.swt.Utils;

/** 
 * Listener primarily for Menu Selection.  Implement run(TableRowCore) and it
 * will get called for each row the user has selected.
 */
public abstract class TableSelectedRowsListener
	extends TableGroupRowRunner
	implements Listener
{
	private final TableView<?> tv;
	private final boolean getOffSWT;

	public TableSelectedRowsListener(TableView<?> impl, boolean getOffSWT) {
		tv = impl;
		this.getOffSWT = getOffSWT;
	}

	/**
	 * triggers the event off of the SWT thread
	 * @param impl
	 */
	public TableSelectedRowsListener(TableView<?> impl) {
		tv = impl;
		this.getOffSWT = true;
	}

	/** Event information passed in via the Listener.  Accessible in 
	 * run(TableRowSWT).
	 */
	protected Event event;

	/** Process the trapped event.  This function does not need to be overidden.
	 * @param e event information
	 */
	public final void handleEvent(Event e) {
		event = e;
		if (getOffSWT) {
			Utils.getOffOfSWTThread(new AERunnable() {
				public void runSupport() {
					tv.runForSelectedRows(TableSelectedRowsListener.this);
				}
			});
		} else {
			tv.runForSelectedRows(this);
		}
	}
}
