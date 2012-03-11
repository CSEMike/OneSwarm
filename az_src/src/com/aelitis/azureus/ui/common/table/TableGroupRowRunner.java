package com.aelitis.azureus.ui.common.table;


/** 
 * Used with {@link TableView#runForSelectedRows}
 */
public abstract class TableGroupRowRunner
{
	/** Code to run 
	 * @param row TableRowCore to run code against
	 */
	public void run(TableRowCore row) {
	}

	/**
	 * Code to run against multiple rows.
	 * 
	 * Return true if this object supports it, false otherwise.
	 * 
	 * @param rows
	 * @return
	 */
	public boolean run(TableRowCore[] rows) {
		return false;
	}
}
