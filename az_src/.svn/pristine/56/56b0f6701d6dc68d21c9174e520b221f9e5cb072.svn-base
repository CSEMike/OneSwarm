package org.gudy.azureus2.ui.swt.speedtest;



import java.util.HashMap;

import org.eclipse.swt.SWT;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCException;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.wizard.AbstractWizardPanel;
import org.gudy.azureus2.ui.swt.wizard.IWizardPanel;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;

public class 
SpeedTestSelector
	extends AbstractWizardPanel<SpeedTestWizard> 
{
	private boolean	mlab_test = true;

	public SpeedTestSelector(SpeedTestWizard wizard, IWizardPanel previous) {
		super(wizard, previous);
	}

	public void show() {
		wizard.setTitle(MessageText.getString("speedtest.wizard.select.title"));
		wizard.setCurrentInfo( "" );
		final Composite rootPanel = wizard.getPanel();
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootPanel.setLayout(layout);

		Composite panel = new Composite(rootPanel, SWT.NULL);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		panel.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		panel.setLayout(layout);

		final Group gRadio = new Group(panel, SWT.NULL);
		Messages.setLanguageText(gRadio, "speedtest.wizard.select.group");
		gRadio.setLayoutData(gridData);
		layout = new GridLayout();
		layout.numColumns = 1;
		gRadio.setLayout( layout );
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gRadio.setLayoutData(gridData);


		// general test

		Button auto_button = new Button (gRadio, SWT.RADIO);
		Messages.setLanguageText(auto_button, "speedtest.wizard.select.general");
		auto_button.setSelection( true );

		// BT

		final Button manual_button = new Button( gRadio, SWT.RADIO );
		Messages.setLanguageText(manual_button, "speedtest.wizard.select.bt");   

		manual_button.addListener(
				SWT.Selection,
				new Listener()
				{
					public void 
					handleEvent(
							Event arg0 ) 
					{
						mlab_test = !manual_button.getSelection();
					}
				});
	}



	public boolean 
	isNextEnabled() 
	{
		return( true );
	}

	public boolean 
	isPreviousEnabled() 
	{
		return( false );
	}

	public IWizardPanel 
	getNextPanel() 
	{
		if ( mlab_test ){

			wizard.close();

			runMLABTest(true, null);
			
			//new ConfigureWizard( false, ConfigureWizard.WIZARD_MODE_SPEED_TEST_AUTO );
			
			return( null );

		}else{

			return( new SpeedTestPanel( wizard, null ));
		}
	}

	public static void runMLABTest(final boolean allowShaperProbeLogic,
			final Runnable runWhenClosed) {
		CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				UIFunctionsManager.getUIFunctions().installPlugin("mlab",
						"dlg.install.mlab", new UIFunctions.actionListener() {
							public void actionComplete(Object result) {
								if (result instanceof Boolean) {
									_runMLABTest(allowShaperProbeLogic, runWhenClosed);
								} else {

									try {
										Throwable error = (Throwable) result;

										Debug.out(error);

									} finally {
										if (runWhenClosed != null) {
											runWhenClosed.run();
										}
									}
								}
							}
						});
			}
		});
	}
	
	private static void _runMLABTest(final boolean allowShaperProbeLogic,
			final Runnable runWhenClosed) {
		PluginInterface pi = AzureusCoreFactory.getSingleton().getPluginManager().getPluginInterfaceByID(
				"mlab");
		try {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("allowShaperProbeLogic", Boolean.valueOf(allowShaperProbeLogic));
			pi.getIPC().invoke("runTest", new Object[] {
				map,
				new IPCInterface() {
					public Object invoke(String methodName, Object[] params)
							throws IPCException {
						// we could set SpeedTest Completed when methodName == "results"
						// or ask user if they want to be prompted again if it isn't
						// But, we'd have to pass a param into runMLABTest (so we don't
						// get prompt on menu invocation).

						// For now, show only once, with no reprompt (even if they cancel).
						// They can use the menu
						COConfigurationManager.setParameter("SpeedTest Completed", true);

						if (runWhenClosed != null) {
							runWhenClosed.run();
						}
						return null;
					}

					public boolean canInvoke(String methodName, Object[] params) {
						return true;
					}
				},
				true
			});

		} catch (Throwable e) {

			Debug.out(e);
			if (runWhenClosed != null) {
				runWhenClosed.run();
			}
		}
	}
}
