package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;

import com.aelitis.azureus.ui.common.ToolBarItem;

public abstract class TableViewTab<DATASOURCETYPE>
	implements UISWTViewCoreEventListener, UIPluginViewToolBarListener,
	AEDiagnosticsEvidenceGenerator
{
	private TableViewSWT<DATASOURCETYPE> tv;
	private Object parentDataSource;
	private final String propertiesPrefix;
	private Composite composite;
	private UISWTView swtView;

	
	public TableViewTab(String propertiesPrefix) {
		this.propertiesPrefix = propertiesPrefix;
	}
	
	public TableViewSWT<DATASOURCETYPE> getTableView() {
		return tv;
	}

	public final void initialize(Composite composite) {
		tv = initYourTableView();
		if (parentDataSource != null) {
			tv.setParentDataSource(parentDataSource);
		}
		Composite parent = initComposite(composite);
		tv.initialize(parent);
		if (parent != composite) {
			this.composite = composite;
		} else {
			this.composite = tv.getComposite();
		}
		
		tableViewTabInitComplete();
	}
	
	public void tableViewTabInitComplete() {
	}

	public Composite initComposite(Composite composite) {
		return composite;
	}

	public abstract TableViewSWT<DATASOURCETYPE> initYourTableView();

	public final void dataSourceChanged(Object newDataSource) {
		this.parentDataSource = newDataSource;
		if (tv != null) {
			tv.setParentDataSource(newDataSource);
		}
	}

	public final void refresh() {
		if (tv != null) {
			tv.refreshTable(false);
		}
	}

	private final void delete() {
		if (tv != null) {
			tv.delete();
		}
	}

	public final String getFullTitle() {
		return MessageText.getString(getPropertiesPrefix() + ".title.full");
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	 */
	public void generate(IndentWriter writer) {
		if (tv != null) {
			tv.generate(writer);
		}
	}
	
	public Composite getComposite() {
		return composite;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	 */
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		if (item.getID().equals("editcolumns")) {
			if (tv instanceof TableViewSWTImpl) {
				((TableViewSWTImpl<?>)tv).showColumnEditor();
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.ToolBarEnabler2#refreshToolBarItems(java.util.Map)
	 */
	public void refreshToolBarItems(Map<String, Long> list) {
		list.put("editcolumns", UIToolBarItem.STATE_ENABLED);
	}
	
	public String getPropertiesPrefix() {
		return propertiesPrefix;
	}
	
	public Menu getPrivateMenu() {
		return null;
	}
	
	public void viewActivated() {
		// cheap hack.. calling isVisible freshens table's visible status (and
		// updates subviews)
		if (tv instanceof TableViewSWTImpl) {
			((TableViewSWTImpl<?>)tv).isVisible();
		}
	}
	
	private void viewDeactivated() {
		if (tv instanceof TableViewSWTImpl) {
			((TableViewSWTImpl<?>)tv).isVisible();
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = (UISWTView) event.getData();
				swtView.setToolBarListener(this);
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite) event.getData());
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				swtView.setTitle(getFullTitle());
				updateLanguage();
				Messages.updateLanguageForControl(composite);
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				dataSourceChanged(event.getData());
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				viewActivated();
				break;

			case UISWTViewEvent.TYPE_FOCUSLOST:
				viewDeactivated();
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
		}

		return true;
	}

	public void updateLanguage() {
	}

	public UISWTView getSWTView() {
		return swtView;
	}
}
