package org.gudy.azureus2.ui.swt.mainwindow;

public interface Refreshable {
	
	  /**
	   * This method is called on each refresh.
	   * The view should not instanciate a Thread to refresh itself, unless this is for async purposes. In which case, don't forget to call the display.asyncexec method.
	   * Called by the GUI Thread
	   */
	  public void refresh();

}
