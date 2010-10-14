package org.gudy.azureus2.platform.macosx.access.cocoa;

/*
 * Created on 27-Mar-2005
 * Created by James Yeh
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

import com.apple.cocoa.foundation.NSAppleEventDescriptor;
import com.apple.cocoa.foundation.NSAppleScript;
import com.apple.cocoa.foundation.NSAutoreleasePool;
import com.apple.cocoa.foundation.NSMutableDictionary;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.macosx.NativeInvocationBridge;

import java.io.File;
import java.text.MessageFormat;

/**
 * <p>Performs PlatformManager tasks using Cocoa-Java (FoundationKit only)</p>
 * <p>For now, operations are performed using NSAppleScript, rather than using NSWorkspace.
 * This is still significantly faster than calling the cmd-line osascript.</p>
 * @version 2.1 Apr 2, 2005
 */
public final class CocoaJavaBridge extends NativeInvocationBridge
{
    /**
     * The path the Cocoa-Java class files are located at
     */
    protected static final String CLASS_PATH = "/system/library/java";

    private static final String REVEAL_SCRIPT_FORMAT = "tell application \"System Events\"\ntell application \"{0}\"\nactivate\nreveal (posix file \"{1}\" as alias)\nend tell\nend tell";

    private static final String DEL_SCRIPT_FORMAT = "tell application \"Finder\" to move (posix file \"{0}\" as alias) to the trash";

    /**
     * Main NSAutoreleasePool
     */
    private int mainPool;

    protected AEMonitor classMon = new AEMonitor("CocoaJavaBridge:C");
    private AEMonitor scriptMon = new AEMonitor("CocoaJavaBridge:S");

    protected boolean isDisposed = false;

    protected RunnableDispatcher scriptDispatcher;

    public CocoaJavaBridge()
    {
        try
        {
            classMon.enter();
            mainPool = NSAutoreleasePool.push();

            scriptDispatcher = new RunnableDispatcher();
        }
        finally
        {
            classMon.exit();
        }
    }

    // interface implementation

    /**
     * {@inheritDoc}
     */
    protected boolean performRecoverableFileDelete(File path)
    {
        if(!path.exists())
            return false;

        NSAppleEventDescriptor result =  executeScriptWithAsync(DEL_SCRIPT_FORMAT, new Object[]{path.getAbsolutePath()});
        return (result != null);
    }

    /**
     * {@inheritDoc}
     */
	protected boolean showInFinder(File path, String fileBrowserApp) {
		if (!path.exists())
			return false;

		NSAppleEventDescriptor result = null;
		int pool = NSAutoreleasePool.push();
		try {
			result = executeScriptWithAsync(REVEAL_SCRIPT_FORMAT, new Object[] {
				fileBrowserApp,
				path.getAbsolutePath()
			});
		} finally {
			NSAutoreleasePool.pop(pool);
		}
		return (result != null);
	}

    /**
     * {@inheritDoc}
     */
    protected boolean isEnabled()
    {
        // simple check with classpath
        return System.getProperty("java.class.path").toLowerCase().indexOf(CLASS_PATH) != -1;
    }

    // class utility methods

    /**
     * <p>Executes a new instance of NSAppleScript</p>
     * <p>The method is wrapped in an autorelease pool and an AEMonitor. If there are
     * no format parameters, MessageFormat is not used to parse the format string, and
     * the format string will be treated as the source itself.</p>
     * @see MessageFormat#format(String, Object...)
     * @see NSAppleScript#execute(com.apple.cocoa.foundation.NSMutableDictionary)
     */
    protected final NSAppleEventDescriptor executeScript(String scriptFormat, Object[] params)
    {
        try
        {
            scriptMon.enter();

            int pool = NSAutoreleasePool.push();
            long start = System.currentTimeMillis();

            String src;
            if(params == null || params.length == 0)
            {
                src = scriptFormat;
            }
            else
            {
                src = MessageFormat.format(scriptFormat, params);
            }

            Debug.outNoStack("Executing: \n" + src);

            NSAppleScript scp = new NSAppleScript(src);
            NSAppleEventDescriptor result =  scp.execute(new NSMutableDictionary());

            Debug.outNoStack(MessageFormat.format("Elapsed time: {0}ms\n", new Object[]{new Long(System.currentTimeMillis() - start)}));
            NSAutoreleasePool.pop(pool);
            return result;
        }
        finally
        {
            scriptMon.exit();
        }
    }

    /**
     * <p>Executes a new instance of NSAppleScript in a forked AEThread</p>
     * <p>This method always returns a "true" event descriptor. Callbacks are currently unsupported
     * , so in the event of an error, the logger is autuomatically notified.</p>
     * <p>The thread's runSupport method is wrapped in an autorelease pool. If there are
     * no format parameters, MessageFormat is not used to parse the format string, and
     * the format string will be treated as the source itself.</p>
     * @see org.gudy.azureus2.core3.util.AEThread#runSupport()
     * @see MessageFormat#format(String, Object...)
     * @see NSAppleScript#execute(com.apple.cocoa.foundation.NSMutableDictionary)
     * @return NSAppleEventDescriptor.descriptorWithBoolean(true)
     */
    protected final NSAppleEventDescriptor executeScriptWithNewThread(final String scriptFormat, final Object[] params)
    {
        Thread worker = new AEThread("ScriptObject", true)
        {
            public void runSupport()
            {
                int pool = NSAutoreleasePool.push();
                long start = System.currentTimeMillis();

                String src;
                if(params == null || params.length == 0)
                {
                    src = scriptFormat;
                }
                else
                {
                    src = MessageFormat.format(scriptFormat, params);
                }

                Debug.outNoStack("Executing: \n" + src);

                NSMutableDictionary errorInfo = new NSMutableDictionary();
                if(new NSAppleScript(src).execute(errorInfo) == null)
                {
                    Debug.out(String.valueOf(errorInfo.objectForKey(NSAppleScript.AppleScriptErrorMessage)));
                    //logWarning(String.valueOf(errorInfo.objectForKey(NSAppleScript.AppleScriptErrorBriefMessage)));
                }

                Debug.outNoStack(MessageFormat.format("Elapsed time: {0}ms\n", new Object[]{new Long(System.currentTimeMillis() - start)}));
                NSAutoreleasePool.pop(pool);
            }
        };

        worker.setPriority(Thread.NORM_PRIORITY - 1);
        worker.start();

        return NSAppleEventDescriptor.descriptorWithBoolean(true);
    }

    /**
     * <p>Asynchronously executes a new instance of NSAppleScript</p>
     * <p>This method always returns a "true" event descriptor. Callbacks are currently unsupported
     * , so in the event of an error, the logger is autuomatically notified.</p>
     * <p>The thread's runSupport method is wrapped in an autorelease pool. If there are
     * no format parameters, MessageFormat is not used to parse the format string, and
     * the format string will be treated as the source itself.</p>
     * @see org.gudy.azureus2.core3.util.AEThread#runSupport()
     * @see MessageFormat#format(String, Object...)
     * @see NSAppleScript#execute(com.apple.cocoa.foundation.NSMutableDictionary)
     * @return NSAppleEventDescriptor.descriptorWithBoolean(true)
     */
    protected final NSAppleEventDescriptor executeScriptWithAsync(final String scriptFormat, final Object[] params)
    {
        final AERunnable worker = new AERunnable()
        {
            public void runSupport()
            {
                int pool = NSAutoreleasePool.push();
                long start = System.currentTimeMillis();

                String src;
                if(params == null || params.length == 0)
                {
                    src = scriptFormat;
                }
                else
                {
                    src = MessageFormat.format(scriptFormat, params);
                }

                Debug.outNoStack("Executing: \n" + src);

                NSMutableDictionary errorInfo = new NSMutableDictionary();
                if(new NSAppleScript(src).execute(errorInfo) == null)
                {
                    Debug.out(String.valueOf(errorInfo.objectForKey(NSAppleScript.AppleScriptErrorMessage)));
                    //logWarning(String.valueOf(errorInfo.objectForKey(NSAppleScript.AppleScriptErrorBriefMessage)));
                }

                Debug.outNoStack(MessageFormat.format("Elapsed time: {0}ms\n", new Object[]{new Long(System.currentTimeMillis() - start)}));
                NSAutoreleasePool.pop(pool);
            }
        };

        AEThread t = new AEThread("ScriptObject", true)
        {
            public void runSupport()
            {
                scriptDispatcher.exec(worker);
            }
        };
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();

        return NSAppleEventDescriptor.descriptorWithBoolean(true);
    }

    /**
     * Logs a warning message to Logger. The class monitor is used.
     * @param message A warning message
     */
    private void logWarning(String message)
    {
        try
        {
            classMon.enter();
            Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_WARNING, message));
        }
        finally
        {
            classMon.exit();
        }
    }

    // disposal

    /**
     * {@inheritDoc}
     */
    protected void dispose()
    {
        try
        {
            classMon.enter();
            if(!isDisposed)
            {
                Debug.outNoStack("Disposing Native PlatformManager...");
                NSAutoreleasePool.pop(mainPool);
                isDisposed = true;
                Debug.outNoStack("Done");
            }
        }
        finally
        {
            classMon.exit();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void finalize() throws Throwable
    {
        dispose();
        super.finalize();
    }

    /**
     * A dispatch object to help facilitate asychronous script execution (from the main thread) in a more
     * predictable fashion.
     */
    private static class RunnableDispatcher
    {
        /**
         * Executes a Runnable object while synchronizing the RunnableDispatcher instance.
         * @param runnable A Runnable
         */
        private void exec(Runnable runnable)
        {
            synchronized(this)
            {
                runnable.run();
            }
        }
    }
}
