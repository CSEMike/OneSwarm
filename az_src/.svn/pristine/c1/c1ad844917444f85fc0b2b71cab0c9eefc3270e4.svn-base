/*
 * File    : UISWTViewImpl.java
 * Created : Oct 14, 2005
 * By      : TuxPaper
 *
 * Copyright (C) 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.awt.Frame;
import java.awt.Panel;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.UIRuntimeException;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.*;

import com.aelitis.azureus.ui.common.ToolBarEnabler;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * This class creates an IView that triggers UISWTViewEventListener 
 * appropriately
 * 
 * @author TuxPaper
 *
 */
public class UISWTViewImpl
	implements UISWTViewCore, AEDiagnosticsEvidenceGenerator
{
	public static final String CFG_PREFIX = "Views.plugins.";

	private PluginUISWTSkinObject skinObject;

	private Object dataSource = null;

	private boolean useCoreDataSource = false;

	private final UISWTViewEventListener eventListener;

	private Composite composite;

	private final String sViewID;

	private int iControlType = UISWTView.CONTROLTYPE_SWT;

	private boolean bFirstGetCompositeCall = true;

	//private final String sParentID;

	private String sTitle = null;

	private String lastFullTitleKey = null;

	private String lastFullTitle = "";

	private Boolean hasFocus = null;

	private UIPluginViewToolBarListener toolbarListener;

	public UISWTViewImpl(String sParentID, String sViewID,
			UISWTViewEventListener eventListener, Object initialDatasource)
			throws Exception {
		//this.sParentID = sParentID;
		this.sViewID = sViewID;
		this.eventListener = eventListener;
		if (eventListener instanceof UISWTViewCoreEventListener) {
			useCoreDataSource = true;
		}

		AEDiagnostics.addEvidenceGenerator(this);

		if (initialDatasource != null) {
			triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, initialDatasource);
		}

		if (!eventListener.eventOccurred(new UISWTViewEventImpl(this,
				UISWTViewEvent.TYPE_CREATE, this)))
			throw new UISWTViewEventCancelledException();

	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getEventListener()
	 */
	public UISWTViewEventListener getEventListener() {
		return eventListener;
	}

	// UISWTPluginView implementation
	// ==============================

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getDataSource()
	 */
	public Object getDataSource() {
		return dataSource;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginView#getViewID()
	 */
	public String getViewID() {
		return sViewID;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginView#closeView()
	 */
	public void closeView() {
		try {

			UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
			if (uiFunctions != null) {
				uiFunctions.closePluginView(this);
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#setControlType(int)
	 */
	public void setControlType(int iControlType) {
		if (iControlType == CONTROLTYPE_AWT || iControlType == CONTROLTYPE_SWT
				|| iControlType == CONTROLTYPE_SKINOBJECT)
			this.iControlType = iControlType;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getControlType()
	 */
	public int getControlType() {
		return iControlType;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#triggerEvent(int, java.lang.Object)
	 */
	public void triggerEvent(int eventType, Object data) {
		// prevent double fire of focus gained/lost
		if (eventType == UISWTViewEvent.TYPE_FOCUSGAINED && hasFocus != null
				&& hasFocus) {
			return;
		}
		if (eventType == UISWTViewEvent.TYPE_FOCUSLOST && hasFocus != null
				&& !hasFocus) {
			return;
		}
		if (eventType == UISWTViewEvent.TYPE_DATASOURCE_CHANGED) {
			Object newDataSource = PluginCoreUtils.convert(data, useCoreDataSource);
			if (dataSource == newDataSource) {
				return;
			}
			data = dataSource = newDataSource;
		} else if (eventType == UISWTViewEvent.TYPE_LANGUAGEUPDATE) {
			lastFullTitle = "";
			Messages.updateLanguageForControl(getComposite());
		}

		try {
			eventListener.eventOccurred(new UISWTViewEventImpl(this, eventType, data));
		} catch (Throwable t) {
			Debug.out("ViewID=" + sViewID + "; EventID=" + eventType + "; data="
					+ data, t);
			//throw (new UIRuntimeException("UISWTView.triggerEvent:: ViewID="
			//		+ sViewID + "; EventID=" + eventType + "; data=" + data, t));
		}
		
		if (eventType == UISWTViewEvent.TYPE_DESTROY) {
			Utils.disposeComposite(getComposite());
		}	
	}

	protected boolean triggerEventRaw(int eventType, Object data) {
		try {
			return eventListener.eventOccurred(new UISWTViewEventImpl(this,
					eventType, data));
		} catch (Throwable t) {
			throw (new UIRuntimeException("UISWTView.triggerEvent:: ViewID="
					+ sViewID + "; EventID=" + eventType + "; data=" + data, t));
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		sTitle = title;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.plugins.UISWTView#getPluginInterface()
	 */
	public PluginInterface getPluginInterface() {
		if (eventListener instanceof UISWTViewEventListenerHolder) {
			return (((UISWTViewEventListenerHolder) eventListener).getPluginInterface());
		}

		return null;
	}

	
	// Core Functions

	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getComposite()
	 */
	public Composite getComposite() {
		if (bFirstGetCompositeCall) {
			bFirstGetCompositeCall = false;
		}
		return composite;
	}


	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getTitleID()
	 */
	public String getTitleID() {
		if (sTitle == null) {
			// still need this crappy check because some plugins still expect their
			// view id to be their name
			if (MessageText.keyExists(sViewID)) {
				return sViewID;
			}
			String id = CFG_PREFIX + sViewID + ".title";
			if (MessageText.keyExists(id)) {
				return id;
			}
			return "!" + sViewID + "!";
		}
		return "!" + sTitle + "!";
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getFullTitle()
	 */
	public String getFullTitle() {
		//System.out.println("getFullTitle " + sTitle + ";" + getTitleID() + ";" + lastFullTitle + ";" + lastFullTitleKey);
		if (sTitle != null) {
			return sTitle;
		}

		String key = getTitleID();
		if (key == null) {
			return "";
		}

		if (lastFullTitle.length() > 0 && key.equals(lastFullTitleKey)) {
			return lastFullTitle;
		}

		lastFullTitleKey = key;

		if (MessageText.keyExists(key) || key.startsWith("!") && key.endsWith("!")) {
			lastFullTitle = MessageText.getString(key);
		} else {
			lastFullTitle = key.replace('.', ' '); // support old plugins
		}

		return lastFullTitle;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#initialize(org.eclipse.swt.widgets.Composite)
	 */
	public void initialize(Composite parent) {
		if (iControlType == UISWTView.CONTROLTYPE_SWT) {
			GridData gridData;
			Layout parentLayout = parent.getLayout();
			if (parentLayout instanceof FormLayout) {
				composite = parent;
			} else {
				composite = new Composite(parent, SWT.NULL);
				GridLayout layout = new GridLayout(1, false);
				layout.marginHeight = 0;
				layout.marginWidth = 0;
				composite.setLayout(layout);
				gridData = new GridData(GridData.FILL_BOTH);
				composite.setLayoutData(gridData);
			}

			triggerEvent(UISWTViewEvent.TYPE_INITIALIZE, composite);

			if (composite.getLayout() instanceof GridLayout) {
				// Force children to have GridData layoutdata.
				Control[] children = composite.getChildren();
				for (int i = 0; i < children.length; i++) {
					Control control = children[i];
					Object layoutData = control.getLayoutData();
					if (layoutData == null || !(layoutData instanceof GridData)) {
						if (layoutData != null)
							Logger.log(new LogEvent(LogIDs.PLUGIN, LogEvent.LT_WARNING,
									"Plugin View '" + sViewID + "' tried to setLayoutData of "
											+ control + " to a " + layoutData.getClass().getName()));

						if (children.length == 1)
							gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
						else
							gridData = new GridData();

						control.setLayoutData(gridData);
					}
				}
			}
		} else if (iControlType == UISWTView.CONTROLTYPE_AWT) {
			composite = new Composite(parent, SWT.EMBEDDED);
			FillLayout layout = new FillLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			composite.setLayout(layout);
			GridData gridData = new GridData(GridData.FILL_BOTH);
			composite.setLayoutData(gridData);

			Frame f = SWT_AWT.new_Frame(composite);

			Panel pan = new Panel();

			f.add(pan);

			triggerEvent(UISWTViewEvent.TYPE_INITIALIZE, pan);
		} else if (iControlType == UISWTViewCore.CONTROLTYPE_SKINOBJECT) {
			triggerEvent(UISWTViewEvent.TYPE_INITIALIZE, getSkinObject());
		}
	}

	/**
	 * @return
	 */
	public boolean requestClose() {
		return triggerEventRaw(UISWTViewEvent.TYPE_CLOSE, null);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#useCoreDataSource()
	 */
	public boolean useCoreDataSource() {
		return useCoreDataSource;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#setUseCoreDataSource(boolean)
	 */
	public void setUseCoreDataSource(boolean useCoreDataSource) {
		if (this.useCoreDataSource == useCoreDataSource) {
			return;
		}

		this.useCoreDataSource = useCoreDataSource;
		triggerEvent(UISWTViewEvent.TYPE_DATASOURCE_CHANGED, dataSource);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#getSkinObject()
	 */
	public PluginUISWTSkinObject getSkinObject() {
		return skinObject;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore#setSkinObject(org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject, org.eclipse.swt.widgets.Composite)
	 */
	public void setSkinObject(PluginUISWTSkinObject skinObject, Composite c) {
		this.skinObject = skinObject;
		this.composite = c;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	 */
	public void generate(IndentWriter writer) {
		if (eventListener instanceof AEDiagnosticsEvidenceGenerator) {
			writer.println("View: " + sViewID + ": " + sTitle);

			try {
				writer.indent();

				((AEDiagnosticsEvidenceGenerator) eventListener).generate(writer);
			} catch (Exception e) {

			} finally {

				writer.exdent();
			}
		} else {
			writer.println("View (no generator): " + sViewID + ": " + sTitle);
		}
	}

	public boolean toolBarItemActivated(ToolBarItem item, long activationType, Object datasource) {
		if (toolbarListener != null) {
			return toolbarListener.toolBarItemActivated(item, activationType, datasource);
		}
		if (eventListener instanceof UIPluginViewToolBarListener) {
			return ((UIPluginViewToolBarListener) eventListener).toolBarItemActivated(item, activationType, datasource);
		} else if (eventListener instanceof ToolBarEnabler) {
			return ((ToolBarEnabler) eventListener).toolBarItemActivated(item.getID());
		} 
		return false;
	}

	public void refreshToolBarItems(Map<String, Long> list) {
		if (eventListener instanceof UIPluginViewToolBarListener) {
			((UIPluginViewToolBarListener) eventListener).refreshToolBarItems(list);
		} else if (eventListener instanceof ToolBarEnabler) {
			Map<String, Boolean> states = new HashMap<String, Boolean>();
			for (String id: list.keySet()) {
				states.put(id, (list.get(id) & UIToolBarItem.STATE_ENABLED) > 0);
			}
			
			((ToolBarEnabler) eventListener).refreshToolBar(states);

			for (String id : states.keySet()) {
				Boolean visible = states.get(id);
				list.put(id, visible ? UIToolBarItem.STATE_ENABLED : 0);
			}
		}
	}

	public void setToolBarListener(UIPluginViewToolBarListener l) {
		toolbarListener = l;
	}
	
	public UIPluginViewToolBarListener getToolBarListener() {
		return toolbarListener;
	}
}
