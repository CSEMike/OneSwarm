package com.aelitis.azureus.ui.common;

import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;

public interface ToolBarItem
	extends UIToolBarItem
{
	public boolean triggerToolBarItem(long activationType, Object datasource);

	public void setDefaultActivationListener(UIToolBarActivationListener toolBarActivation);

	public void setAlwaysAvailable(boolean b);

	public String getTooltipID();

	public UIToolBarActivationListener getDefaultActivationListener();
}
