package org.gudy.azureus2.ui.swt.views.clientstats;

import org.gudy.azureus2.plugins.ui.tables.*;

public class ColumnCS_Count
	implements TableCellRefreshListener
{

	public static final String COLUMN_ID = "count";

	public ColumnCS_Count(TableColumn column) {
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 50);
		column.addListeners(this);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
	}

	public void refresh(TableCell cell) {
		ClientStatsDataSource ds = (ClientStatsDataSource) cell.getDataSource();
		if (ds == null) {
			return;
		}
		long val = ds.count;
		if (cell.setSortValue(val) || !cell.isValid()) {
			cell.setText(Long.toString(val));
		}
	}
}
