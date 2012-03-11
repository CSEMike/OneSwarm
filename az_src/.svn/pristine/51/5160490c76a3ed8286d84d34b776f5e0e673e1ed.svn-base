package org.gudy.azureus2.ui.swt.pluginsimpl;

import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

/**
 * A holding area between the public UISWTView plugin interface,
 * and things that we may eventually move into UISWTView
 * 
 */
public interface UISWTViewCore
	extends UISWTView
{
	public static final int CONTROLTYPE_SKINOBJECT = 0x100 + 1;

	// >> From IView
  /**
   * This method is called when the view is instanciated, it should initialize all GUI
   * components. Must NOT be blocking, or it'll freeze the whole GUI.
   * Caller is the GUI Thread.
   * 
   * @param composite the parent composite. Each view should create a child 
   *         composite, and then use this child composite to add all elements
   *         to.
   *         
   * @note It's possible that the view may be created, but never initialize'd.
   *        In these cases, delete will still be called.
   */
  public void initialize(Composite composite);
  
  /**
   * This method is called after initialize so that the Tab is set its control
   * Caller is the GUI Thread.
   * @return the Composite that should be set as the control for the Tab item
   */
  public Composite getComposite();
  
  /**
   * Messagebundle ID for title
   */
  public String getTitleID();
  
  /**
   * Called in order to set / update the title of this View.  When the view
   * is being displayed in a tab, the full title is used for the tooltip.
   * 
   * @return the full title for the view
   */
  public String getFullTitle();
  
  // << From IView

	public void setSkinObject(PluginUISWTSkinObject so, Composite composite);
	
	public PluginUISWTSkinObject getSkinObject();
	
	public void setUseCoreDataSource(boolean useCoreDataSource);

	public boolean useCoreDataSource();

	public UISWTViewEventListener getEventListener();
}
