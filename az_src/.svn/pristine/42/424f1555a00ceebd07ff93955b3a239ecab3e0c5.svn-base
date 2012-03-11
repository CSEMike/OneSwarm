/**
 * 
 */
package com.aelitis.azureus.ui.swt.mdi;

import org.eclipse.swt.graphics.Image;

import org.gudy.azureus2.plugins.ui.UIPluginView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCore;

import com.aelitis.azureus.ui.mdi.MdiEntry;

/**
 * @author TuxPaper
 * @created Jan 29, 2010
 *
 */
public interface MdiEntrySWT
	extends MdiEntry
{
	//public SWTSkinObject getSkinObject();

	public UIPluginView getView();

	public UISWTViewCore getCoreView();

	public UISWTViewEventListener getEventListener();

	public void addListener(MdiSWTMenuHackListener l);

	public void removeListener(MdiSWTMenuHackListener l);

	void setImageLeft(Image imageLeft);
}
