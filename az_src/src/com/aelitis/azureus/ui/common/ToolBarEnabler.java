package com.aelitis.azureus.ui.common;

import java.util.Map;

import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarEnablerBase;

// Change with caution: Some internal plugins use this directly
public interface ToolBarEnabler
	extends UIToolBarEnablerBase
{
	public void refreshToolBar(Map<String, Boolean> list);

	public boolean toolBarItemActivated(String itemKey);
}
