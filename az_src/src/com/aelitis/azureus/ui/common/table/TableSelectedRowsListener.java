package com.aelitis.azureus.ui.common.table;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/** 
 * Listener primarily for Menu Selection.  Implement run(TableRowCore) and it
 * will get called for each row the user has selected.
 */
public abstract class TableSelectedRowsListener
	extends TableGroupRowRunner
	implements Listener
{
	/**
	 * 
	 */
	private final TableView tv;

	/**
	 * @param impl
	 */
	public TableSelectedRowsListener(TableView impl) {
		tv = impl;
	}

	/** Event information passed in via the Listener.  Accessible in 
	 * run(TableRowSWT).
	 */
	protected Event event;

	/** Process the trapped event.  This function does not need to be overidden.
	 * @param e event information
	 */
	public void handleEvent(Event e) {
		event = e;
		tv.runForSelectedRows(this);
	}
}
