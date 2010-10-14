package edu.uw.cse.netlab.utils;

import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreLifecycleListener;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

public abstract class CoreWaiter
{
	private static Logger logger = Logger.getLogger(CoreWaiter.class.getName());
	public CoreWaiter()
	{
		if( AzureusCoreImpl.isCoreAvailable() == false )
		{
			logger.warning("core not available yet. can't start!");
			return;
		}
		
		if( AzureusCoreImpl.getSingleton().isStarted() == false )
		{
			logger.warning("Core not started yet, listening...");
			AzureusCoreImpl.getSingleton().addLifecycleListener(new AzureusCoreLifecycleListener(){
				public void componentCreated( AzureusCore core,
						AzureusCoreComponent component ) {}

				public boolean requiresPluginInitCompleteBeforeStartedEvent() {
					return false;
				}

				public boolean restartRequested( AzureusCore core )
						throws AzureusCoreException {
					return false;
				}

				public void started( AzureusCore core ) {
					logger.fine("got start");
					init();
				}

				public boolean stopRequested( AzureusCore core )
						throws AzureusCoreException {
					return false;
				}

				public void stopped( AzureusCore core ) {}

				public void stopping( AzureusCore core ) {}

				public boolean syncInvokeRequired() {
					return false;
				}});
		}
		else
			init();
	}
	
	protected abstract void init();
}
