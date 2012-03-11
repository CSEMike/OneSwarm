package org.gudy.azureus2.ui.swt.views.clientstats;

import org.gudy.azureus2.core3.util.DisplayFormatters;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;

public class ColumnCS_Pct
	implements TableCellRefreshListener
{

	public static final String COLUMN_ID = "percent";

	public ColumnCS_Pct(TableColumn column) {
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 50);
		column.addListeners(this);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
		column.setRefreshInterval(TableColumn.INTERVAL_LIVE);
	}

	public void refresh(TableCell cell) {
		ClientStatsDataSource ds = (ClientStatsDataSource) cell.getDataSource();
		if (ds == null) {
			return;
		}
		float val = ds.count * 1000f / ds.overall.count;
		if (cell.setSortValue(val) || !cell.isValid()) {
			cell.setText(DisplayFormatters.formatPercentFromThousands((int) val));
		}
	}
}
