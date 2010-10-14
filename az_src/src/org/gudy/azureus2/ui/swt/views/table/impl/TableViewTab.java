package org.gudy.azureus2.ui.swt.views.table.impl;

import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;

public class TableViewTab extends AbstractIView
{
	private TableViewSWT tv;

	public void setTableView(TableViewSWT tv) {
		this.tv = tv;
	}
	
	public TableViewSWT getTableView() {
		return tv;
	}

	public final void initialize(Composite composite) {
		tv.initialize(composite);
	}

	public final void dataSourceChanged(Object newDataSource) {
		tv.setParentDataSource(newDataSource);
	}

	public void updateLanguage() {
		super.updateLanguage();
		tv.updateLanguage();
	}

	public final void refresh() {
		tv.refreshTable(false);
	}

	// @see org.gudy.azureus2.ui.swt.views.AbstractIView#delete()
	public final void delete() {
		tv.delete();
		super.delete();
	}

	// @see org.gudy.azureus2.ui.swt.views.AbstractIView#getData()
	public final String getData() {
		return tv.getPropertiesPrefix() + ".title.short";
	}

	public final String getFullTitle() {
		return MessageText.getString(tv.getPropertiesPrefix() + ".title.full");
	}

	// @see org.gudy.azureus2.ui.swt.views.AbstractIView#generateDiagnostics(org.gudy.azureus2.core3.util.IndentWriter)
	public final void generateDiagnostics(IndentWriter writer) {
		tv.generate(writer);
	}
	
	// @see org.gudy.azureus2.ui.swt.views.AbstractIView#getComposite()
	public Composite getComposite() {
		return tv.getComposite();
	}
}
