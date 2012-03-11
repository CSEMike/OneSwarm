package org.gudy.azureus2.ui.swt.shells;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface IWizardPage
{
	public String getPageID();

	public boolean isComplete();

	public boolean setComplete();

	public void performFinish();

	public void performDispose();

	public String getTitle();

	public String getDesciption();

	public String getWindowTitle();

	public Composite createControls(Composite parent);

	public Control getControl();

	public void performAboutToBeHidden();

	public void performAboutToBeShown();

	public MultipageWizard getWizard();
	
	public boolean isInitOnStartup();

}
