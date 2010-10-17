package org.gudy.azureus2.ui.swt.osx;

import java.lang.reflect.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.C;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.mainwindow.SWTThread;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.nat.NatTestWindow;
import org.gudy.azureus2.ui.swt.speedtest.SpeedTestWizard;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

/**
 * You can exclude this file (or this whole path) for non OSX builds
 * 
 * Hook some Cocoa specific abilities:
 * - App->About        <BR>
 * - App->Preferences  <BR>
 * - App->Exit         <BR>
 * <BR>
 * - OpenDocument  (possible limited to only files?) <BR>
 *
 * This code was influenced by the
 * <a href="http://www.transparentech.com/opensource/cocoauienhancer">
 * CocoaUIEnhancer</a>, which was influenced by the 
 * <a href="http://www.simidude.com/blog/2008/macify-a-swt-application-in-a-cross-platform-way/">
 * CarbonUIEnhancer from Agynami</a>.
 * 
 * Both cocoa implementations are modified from 
 * <a href="http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.ui.cocoa/src/org/eclipse/ui/internal/cocoa/CocoaUIEnhancer.java">
 * org.eclipse.ui.internal.cocoa.CocoaUIEnhancer</a>
 */
public class CocoaUIEnhancer
{
	public static final boolean is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
	
	private static final boolean DEBUG = false;

	private static Object /*Callback*/ callBack3;

	private static long callBack3Addr;

	private static Object /*Callback*/ callBack4;

	private static long callBack4Addr;

	private static CocoaUIEnhancer instance;

	private static final int kAboutMenuItem = 0;

	private static final int kPreferencesMenuItem = 2;

	private static final int kServicesMenuItem = 4;

	// private static final int kHideApplicationMenuItem = 6;

	// private static final int kQuitMenuItem = 10;

	//private static int NSWindowCloseButton = 0;

	//private static int NSWindowDocumentIconButton = 4;

	//private static int NSWindowMiniaturizeButton = 1;

	private static int NSWindowToolbarButton = 3;

	//private static int NSWindowZoomButton = 2;

	private static long sel_aboutMenuItemSelected_;

	private static long sel_application_openFile_;

	private static long sel_application_openFiles_;

	private static long sel_preferencesMenuItemSelected_;
	
	private static long sel_applicationShouldHandleReopen_;

	private static long sel_toolbarButtonClicked_;

	private static long sel_restartMenuSelected_;

	private static long sel_wizardMenuSelected_;

	private static long sel_natMenuSelected_;

	private static long sel_speedMenuSelected_;
	
	private static boolean alreadyHaveOpenDoc;

	static final byte[] SWT_OBJECT = {
		'S',
		'W',
		'T',
		'_',
		'O',
		'B',
		'J',
		'E',
		'C',
		'T',
		'\0'
	};

	private long delegateIdSWTApplication;

	private long delegateJniRef;

	private Object delegate;

	private static boolean initialized = false;

	private static Class<?> osCls = classForName("org.eclipse.swt.internal.cocoa.OS");
	private static Class<?> nsmenuCls = classForName("org.eclipse.swt.internal.cocoa.NSMenu");
	private static Class<?> nsmenuitemCls = classForName("org.eclipse.swt.internal.cocoa.NSMenuItem");
	private static Class<?> nsapplicationCls = classForName("org.eclipse.swt.internal.cocoa.NSApplication");
	private static Class<?> nsarrayCls = classForName("org.eclipse.swt.internal.cocoa.NSArray");
	private static Class<?> nsstringCls = classForName("org.eclipse.swt.internal.cocoa.NSString");
	private static Class<?> swtmenuitemCls = classForName("org.eclipse.swt.internal.cocoa.SWTMenuItem");
	private static Class<?> nsidCls = classForName("org.eclipse.swt.internal.cocoa.id");
	private static Class<?> nsautoreleasepoolCls = classForName("org.eclipse.swt.internal.cocoa.NSAutoreleasePool");
	private static Class<?> nsworkspaceCls = classForName("org.eclipse.swt.internal.cocoa.NSWorkspace");
	private static Class<?> nsimageCls = classForName("org.eclipse.swt.internal.cocoa.NSImage");
	private static Class<?> nssizeCls = classForName("org.eclipse.swt.internal.cocoa.NSSize");

	static {
		
		Class<CocoaUIEnhancer> clazz = CocoaUIEnhancer.class;
		Class<?> callbackCls = classForName("org.eclipse.swt.internal.Callback");

		try {
			SWT.class.getDeclaredField("OpenDocument");
			alreadyHaveOpenDoc = true;
		} catch (Throwable t) {
			alreadyHaveOpenDoc = false;
		}

		try {
			Method mGetAddress = callbackCls.getMethod("getAddress", new Class[0]);
			Constructor<?> consCallback = callbackCls.getConstructor(new Class<?>[] {
				Object.class,
				String.class,
				int.class
			});
//			callBack3 = new Callback(clazz, "actionProc", 3);
			callBack3 = consCallback.newInstance(new Object[] {
				clazz,
				"actionProc",
				3
			});
			Object object = mGetAddress.invoke(callBack3, (Object[]) null);
			callBack3Addr = convertToLong(object);
			if (callBack3Addr == 0) {
				SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
			}

			//callBack4 = new Callback(clazz, "actionProc", 4);
			callBack4 = consCallback.newInstance(new Object[] {
				clazz,
				"actionProc",
				4
			});
			object = mGetAddress.invoke(callBack4, (Object[]) null);
			callBack4Addr = convertToLong(object);
			if (callBack4Addr == 0) {
				SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
			}
		} catch (Throwable e) {
			Debug.out(e);
		}
	}
	
	/** Will be used with 64-bit SWT. */
	static long actionProc(long id, long sel, long arg0) {
		return (long)actionProc((int) id, (int) sel, (int) arg0);
	}

	static int actionProc(int id, int sel, int arg0) {
		if (DEBUG) {
			System.err.println("id=" + id + ";sel=" + sel);
		}

		if (sel == sel_aboutMenuItemSelected_) {
			AboutWindow.show(SWTThread.getInstance().getDisplay());
		} else if (sel == sel_restartMenuSelected_) {
			UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
			if (uiFunctions != null) {
				uiFunctions.dispose(true, false);
			}
		} else if (sel == sel_wizardMenuSelected_) {
			new ConfigureWizard(AzureusCoreImpl.getSingleton(), false);
		} else if (sel == sel_natMenuSelected_) {
			new NatTestWindow();
		} else if (sel == sel_speedMenuSelected_) {
			new SpeedTestWizard(AzureusCoreImpl.getSingleton(), SWTThread.getInstance().getDisplay());
		} else if (sel == sel_toolbarButtonClicked_) {
			try {
				Field fldsel_window = osCls.getField("sel_window");
				Object windowId = invoke(osCls, "objc_msgSend", new Object[] {
					wrapPointer(arg0),
					fldsel_window.get(null)
				});
				final Shell shellAffected = (Shell) invoke(Display.class,
						Display.getCurrent(), "findWidget", new Object[] {
							windowId
						});

				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						int type;
						Long l = (Long) shellAffected.getData("OSX.ToolBarToggle");
						if (l == null || l.longValue() == 0) {
							type = SWT.Collapse;
						} else {
							type = SWT.Expand;
						}

						Event event = new Event();
						event.type = type;
						event.display = shellAffected.getDisplay();
						event.widget = shellAffected;
						shellAffected.notifyListeners(type, event);

						shellAffected.setData("OSX.ToolBarToggle", new Long(
								type == SWT.Collapse ? 1 : 0));
					}
				});
			} catch (Throwable t) {
				Debug.out(t);
			}

		} else if (sel == sel_preferencesMenuItemSelected_) {
			UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
          	 if (uiFunctions != null) {
          		 uiFunctions.showConfig(null);
          	 }

		}
		return 0;
	}
	
	/** Will be used with 64-bit SWT. */
	static long actionProc(long id, long sel,
			long arg0, long arg1)
			throws Throwable {
		return (long)actionProc((int) id, (int) sel, (int) arg0, (int) arg1); 
	}

	static int /*long*/actionProc(int /*long*/id, int /*long*/sel,
			int /*long*/arg0, int /*long*/arg1)
			throws Throwable {
		if (DEBUG) {
			System.err.println("actionProc 4 " + id + "/" + sel);
		}
		Display display = Display.getCurrent();
		if (display == null)
			return 0;

		if (!alreadyHaveOpenDoc && sel == sel_application_openFile_) {
			Constructor<?> conNSString = nsstringCls.getConstructor(new Class[] {
				int.class
			});
			Object file = conNSString.newInstance(arg1);
			String fileString = (String) invoke(file, "getString");
			if (DEBUG) {
				System.err.println("OMG GOT OpenFile " + fileString);
			}
			fileOpen(new String[] {
				fileString
			});
		} else if (!alreadyHaveOpenDoc && sel == sel_application_openFiles_) {
			Constructor<?> conNSArray = nsarrayCls.getConstructor(new Class[] {
				int.class
			});
			Constructor<?> conNSString = nsstringCls.getConstructor(new Class[] {
				nsidCls
			});

			Object arrayOfFiles = conNSArray.newInstance(arg1);
			int count = ((Number) invoke(arrayOfFiles, "count")).intValue();

			String[] files = new String[count];
			for (int i = 0; i < count; i++) {
				Object fieldId = invoke(nsarrayCls, arrayOfFiles, "objectAtIndex",
						new Object[] {
							i
						});
				Object nsstring = conNSString.newInstance(fieldId);
				files[i] = (String) invoke(nsstring, "getString");

				if (DEBUG) {
					System.err.println("OMG GOT OpenFiles " + files[i]);
				}
			}
			fileOpen(files);
		} else if (sel == sel_applicationShouldHandleReopen_) {
			Event event = new Event ();
			event.detail = 1;
			if (display != null) {
				invoke(Display.class, display, "sendEvent", new Class[] {
					int.class,
					Event.class
				}, new Object[] {
					SWT.Activate,
					event
				});
			}
		}
		return 0;
	}

	private static Class<?> classForName(String classname) {
		try {
			Class<?> cls = Class.forName(classname);
			return cls;
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private static long convertToLong(Object object) {
		if (object instanceof Integer) {
			Integer i = (Integer) object;
			return i.longValue();
		}
		if (object instanceof Long) {
			Long l = (Long) object;
			return l.longValue();
		}
		return 0;
	}

	protected static void fileOpen(final String[] files) {
//		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
//			public void azureusCoreRunning(AzureusCore core) {
				TorrentOpener.openTorrents(files);
//			}
//		});
	}

	public static CocoaUIEnhancer getInstance() {
		if (instance == null) {
			try {
				instance = new CocoaUIEnhancer();
			} catch (Throwable e) {
				Debug.out(e);
			}
		}
		return instance;
	}

	private static Object invoke(Class<?> clazz, Object target,
			String methodName, Object[] args) {
		try {
			Class<?>[] signature = new Class<?>[args.length];
			for (int i = 0; i < args.length; i++) {
				Class<?> thisClass = args[i].getClass();
				if (thisClass == Integer.class)
					signature[i] = int.class;
				else if (thisClass == Long.class)
					signature[i] = long.class;
				else if (thisClass == Byte.class)
					signature[i] = byte.class;
				else if (thisClass == Boolean.class)
					signature[i] = boolean.class;
				else
					signature[i] = thisClass;
			}
			Method method = clazz.getMethod(methodName, signature);
			return method.invoke(target, args);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static Object invoke(Class<?> clazz, Object target,
			String methodName, Class[] signature, Object[] args) {
		try {
			Method method = clazz.getDeclaredMethod(methodName, signature);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static Object invoke(Class<?> clazz, String methodName, Object[] args) {
		return invoke(clazz, null, methodName, args);
	}

	private static Object invoke(Object obj, String methodName) {
		return invoke(obj, methodName, (Class<?>[]) null, (Object[]) null);
	}

	private static Object invoke(Object obj, String methodName,
			Class<?>[] paramTypes, Object... arguments) {
		try {
			Method m = obj.getClass().getMethod(methodName, paramTypes);
			return m.invoke(obj, arguments);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static long registerName(Class<?> osCls, String name)
			throws IllegalArgumentException, SecurityException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Object object = invoke(osCls, "sel_registerName", new Object[] {
			name
		});
		return convertToLong(object);
	}

	////////////////////////////////////////////////////////////

	private static Object wrapPointer(long value) {
		Class<?> PTR_CLASS = C.PTR_SIZEOF == 8 ? long.class : int.class;
		if (PTR_CLASS == long.class)
			return new Long(value);
		else
			return new Integer((int) value);
	}

	private CocoaUIEnhancer()
			throws Throwable {

		// Instead of creating a new delegate class in objective-c,
		// just use the current SWTApplicationDelegate. An instance of this
		// is a field of the Cocoa Display object and is already the target
		// for the menuItems. So just get this class and add the new methods
		// to it.
		Object delegateObjSWTApplication = invoke(osCls, "objc_lookUpClass",
				new Object[] {
					"SWTApplicationDelegate"
				});
		delegateIdSWTApplication = convertToLong(delegateObjSWTApplication);

		// This doesn't feel right, but it works
		Class<?> swtapplicationdelegateCls = classForName("org.eclipse.swt.internal.cocoa.SWTApplicationDelegate");
		delegate = swtapplicationdelegateCls.newInstance();
		Object delegateAlloc = invoke(delegate, "alloc");
		invoke(delegateAlloc, "init");
		Object delegateIdObj = nsidCls.getField("id").get(delegate);
		delegateJniRef = ((Number) invoke(osCls, "NewGlobalRef", new Class<?>[] {
			Object.class
		}, new Object[] {
			CocoaUIEnhancer.this
		})).longValue();
		if (delegateJniRef == 0)
			SWT.error(SWT.ERROR_NO_HANDLES);
		//OS.object_setInstanceVariable(delegate.id, SWT_OBJECT, delegateJniRef);
		invoke(osCls, "object_setInstanceVariable", new Object[] {
			delegateIdObj,
			SWT_OBJECT,
			wrapPointer(delegateJniRef)
		});
	}

	/**
	 * Hook the given Listener to the Mac OS X application Quit menu and the IActions to the About
	 * and Preferences menus.
	 * 
	 * @param display
	 *            The Display to use.
	 * @param quitListener
	 *            The listener to invoke when the Quit menu is invoked.
	 * @param aboutAction
	 *            The action to run when the About menu is invoked.
	 * @param preferencesAction
	 *            The action to run when the Preferences menu is invoked.
	 */
	public void hookApplicationMenu() {
		Display display = Display.getCurrent();
		try {
			// Initialize the menuItems.
			initialize();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		// Schedule disposal of callback object
		display.disposeExec(new Runnable() {
			public void run() {
				invoke(callBack3, "dispose");
				callBack3 = null;
				invoke(callBack4, "dispose");
				callBack4 = null;

				if (delegateJniRef != 0) {
					//OS.DeleteGlobalRef(delegateJniRef);
					invoke(osCls, "DeleteGlobalRef", new Object[] {
						wrapPointer(delegateJniRef)
					});
					delegateJniRef = 0;
				}

				if (delegate != null) {
					invoke(delegate, "release");
					delegate = null;
				}
			}
		});
	}

	public void hookDocumentOpen()
			throws Throwable {
		
		if (alreadyHaveOpenDoc) {
			return;
		}

		if (sel_application_openFile_ == 0) {
			sel_application_openFile_ = registerName(osCls, "application:openFile:");
		}
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_application_openFile_),
			wrapPointer(callBack4Addr),
			"@:@:@"
		});

		if (sel_application_openFiles_ == 0) {
			sel_application_openFiles_ = registerName(osCls, "application:openFiles:");
		}
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_application_openFiles_),
			wrapPointer(callBack4Addr),
			"@:@:@"
		});
	}

	private void initialize()
			throws Exception {


		// Register names in objective-c.
		if (sel_preferencesMenuItemSelected_ == 0) {
			sel_preferencesMenuItemSelected_ = registerName(osCls,
					"preferencesMenuItemSelected:");
			sel_aboutMenuItemSelected_ = registerName(osCls, "aboutMenuItemSelected:");
			sel_restartMenuSelected_ = registerName(osCls, "restartMenuItemSelected:");
			sel_natMenuSelected_ = registerName(osCls, "natMenuItemSelected:");
			sel_speedMenuSelected_ = registerName(osCls, "speedMenuItemSelected:");
			sel_wizardMenuSelected_ = registerName(osCls, "wizardMenuItemSelected:");
			sel_applicationShouldHandleReopen_ = registerName(osCls, "applicationShouldHandleReopen:hasVisibleWindows:");
		}

		// Add the action callbacks for Preferences and About menu items.
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_preferencesMenuItemSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_aboutMenuItemSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_restartMenuSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_wizardMenuSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_speedMenuSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_natMenuSelected_),
			wrapPointer(callBack3Addr),
			"@:@"
		});
		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_applicationShouldHandleReopen_),
			wrapPointer(callBack4Addr),
			"@:@c"
		});

		// Get the Mac OS X Application menu.
		Object sharedApplication = invoke(nsapplicationCls, "sharedApplication");
		Object mainMenu = invoke(sharedApplication, "mainMenu");
		Object mainMenuItem = invoke(nsmenuCls, mainMenu, "itemAtIndex",
				new Object[] {
					wrapPointer(0)
				});
		Object appMenu = invoke(mainMenuItem, "submenu");

		// Create the About <application-name> menu command
		Object aboutMenuItem = invoke(nsmenuCls, appMenu, "itemAtIndex",
				new Object[] {
					wrapPointer(kAboutMenuItem)
				});

		// Enable the Preferences menuItem.
		Object prefMenuItem = invoke(nsmenuCls, appMenu, "itemAtIndex",
				new Object[] {
					wrapPointer(kPreferencesMenuItem)
				});
		invoke(nsmenuitemCls, prefMenuItem, "setEnabled", new Object[] {
			true
		});

		Object servicesMenuItem = invoke(nsmenuCls, appMenu, "itemAtIndex",
				new Object[] {
					wrapPointer(kServicesMenuItem)
				});
		invoke(nsmenuitemCls, servicesMenuItem, "setEnabled", new Object[] {
			false
		});

		// Set the action to execute when the About or Preferences menuItem is invoked.
		//
		// We don't need to set the target here as the current target is the SWTApplicationDelegate
		// and we have registerd the new selectors on it. So just set the new action to invoke the
		// selector.
		invoke(nsmenuitemCls, prefMenuItem, "setAction", new Object[] {
			wrapPointer(sel_preferencesMenuItemSelected_)
		});
		invoke(nsmenuitemCls, aboutMenuItem, "setAction", new Object[] {
			wrapPointer(sel_aboutMenuItemSelected_)
		});

		// Add other menus
		Object menuId = appMenu.getClass().getField("id").get(appMenu);
		boolean isAZ3 = "az3".equalsIgnoreCase(COConfigurationManager.getStringParameter("ui"));

		if (!isAZ3) {
			// add Wizard, NAT Test, Speed Test

			addMenuItem(menuId, 5, (int) sel_wizardMenuSelected_,
					MessageText.getString("MainWindow.menu.file.configure").replaceAll(
							"&", ""));

			addMenuItem(menuId, 6, (int) sel_natMenuSelected_, MessageText.getString(
					"MainWindow.menu.tools.nattest").replaceAll("&", ""));

			addMenuItem(menuId, 7, (int) sel_speedMenuSelected_,
					MessageText.getString("MainWindow.menu.tools.speedtest").replaceAll(
							"&", ""));

		}

		int numOfItems = ((Number) invoke(appMenu, "numberOfItems")).intValue();

		Object sep = invoke(osCls, "objc_msgSend", new Object[] {
			osCls.getField("class_NSMenuItem").get(null),
			osCls.getField("sel_separatorItem").get(null)
		});
		invoke(osCls, "objc_msgSend", new Object[] {
			sep,
			osCls.getField("sel_retain").get(null)
		});
		//OS.objc_msgSend(menuId, OS.sel_insertItem_atIndex_, sep, numOfItems - 1);
		invoke(osCls, "objc_msgSend", new Object[] {
			menuId,
			osCls.getField("sel_insertItem_atIndex_").get(null),
			sep,
			is64bit ? (long) (numOfItems - 1) : (numOfItems - 1)
		});

		numOfItems++;

		addMenuItem(menuId, numOfItems - 1, (int) sel_restartMenuSelected_,
				MessageText.getString("MainWindow.menu.file.restart").replaceAll("&",
						""));
		
		initialized = true;
	}

	private void addMenuItem(Object menuId, int index, int selector, String title) {
		try {
			//NSMenuItem nsItem = (NSMenuItem) new SWTMenuItem().alloc();
			Object oSWTMenuItem = swtmenuitemCls.newInstance();
			Object nsItem = invoke(oSWTMenuItem, "alloc");

			Object nsStrTitle = invoke(nsstringCls, "stringWith", new Object[] {
				title
			});
			Object nsStrEmpty = invoke(nsstringCls, "stringWith", new Object[] {
				""
			});
			invoke(nsmenuitemCls, nsItem, "initWithTitle", new Object[] {
				nsStrTitle,
				is64bit ? 0L : 0,
				nsStrEmpty
			});
			invoke(nsItem, "setTarget", new Class<?>[] {
				nsidCls
			}, new Object[] {
				delegate
			});
			invoke(nsmenuitemCls, nsItem, "setAction", new Object[] {
				wrapPointer(selector)
			});

			//OS.objc_msgSend(menuId, OS.sel_insertItem_atIndex_, nsItem.id, index);
			invoke(osCls, "objc_msgSend", new Object[] {
				menuId,
				osCls.getField("sel_insertItem_atIndex_").get(null),
				nsmenuitemCls.getField("id").get(nsItem),
				is64bit ? (long) index : index
			});
		} catch (Throwable e) {
			Debug.out(e);
		}
	}

	private Object invoke(Class<?> cls, String methodName) {
		return invoke(cls, methodName, (Class<?>[]) null, (Object[]) null);
	}

	private Object invoke(Class<?> cls, String methodName, Class<?>[] paramTypes,
			Object... arguments) {
		try {
			Method m = cls.getMethod(methodName, paramTypes);
			return m.invoke(null, arguments);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void registerToolbarToggle(Shell shell)
			throws Throwable {

		if (sel_toolbarButtonClicked_ == 0) {
			sel_toolbarButtonClicked_ = registerName(osCls, "toolbarButtonClicked:");
		}

		invoke(osCls, "class_addMethod", new Object[] {
			wrapPointer(delegateIdSWTApplication),
			wrapPointer(sel_toolbarButtonClicked_),
			wrapPointer(callBack3Addr),
			"@:@"
		});

		Class<?> nstoolbarCls = classForName("org.eclipse.swt.internal.cocoa.NSToolbar");
		Class<?> nsbuttonCls = classForName("org.eclipse.swt.internal.cocoa.NSButton");

		//NSToolbar dummyBar = new NSToolbar();
		Object dummyBar = nstoolbarCls.newInstance();
		//dummyBar.alloc();
		invoke(dummyBar, "alloc");
		//dummyBar.initWithIdentifier(NSString.stringWith("SWTToolbar"));
		Object nsStrDummyToolbar = invoke(nsstringCls, "stringWith", new Object[] {
			"SWTToolbar"
		});
		invoke(dummyBar, "initWithIdentifier", new Class<?>[] {
			nsstringCls
		}, new Object[] {
			nsStrDummyToolbar
		});
		//dummyBar.setVisible(false);
		invoke(dummyBar, "setVisible", new Class<?>[] {
			boolean.class
		}, new Object[] {
			Boolean.FALSE
		});

		// reflect me
		//NSWindow nsWindow = shell.view.window();
		Object view = shell.getClass().getField("view").get(shell);
		Object nsWindow = invoke(view, "window");
		//nsWindow.setToolbar(dummyBar);
		invoke(nsWindow, "setToolbar", new Class<?>[] {
			nstoolbarCls
		}, new Object[] {
			dummyBar
		});
		//nsWindow.setShowsToolbarButton(true);
		invoke(nsWindow, "setShowsToolbarButton", new Class<?>[] {
			boolean.class
		}, new Object[] {
			Boolean.TRUE
		});

		//NSButton toolbarButton = nsWindow.standardWindowButton(NSWindowToolbarButton);
		Object toolbarButton = invoke(nsWindow, "standardWindowButton",
				new Class<?>[] {
					int.class
				}, new Object[] {
					new Integer(NSWindowToolbarButton)
				});

		//toolbarButton.setTarget(delegate);
		invoke(toolbarButton, "setTarget", new Class[] {
			nsidCls
		}, new Object[] {
			delegate
		});

		//OS.objc_msgSend(this.id, OS.sel_setTarget_, anObject != null ? anObject.id : 0);
		//invoke(osCls, "objc_msgSend", new Object[] {
		//	toolbarButton.getClass().getField("id").get(toolbarButton),
		//	osCls.getField("sel_setTarget_").get(null),
		//	wrapPointer(delegateIdSWTApplication)
		//});

		//toolbarButton.setAction((int) sel_toolbarButtonClicked_);
		invoke(nsbuttonCls, toolbarButton, "setAction", new Object[] {
			wrapPointer(sel_toolbarButtonClicked_)
		});
	}

	// from Program.getImageData, except returns bigger images
	public static Image getFileIcon (String path, int imageWidthHeight) {
		Object pool = null;
		try {
			//NSAutoreleasePool pool = (NSAutoreleasePool) new NSAutoreleasePool().alloc().init();
			pool = nsautoreleasepoolCls.newInstance();
			Object delegateAlloc = invoke(pool, "alloc");
			invoke(delegateAlloc, "init");

			//NSWorkspace workspace = NSWorkspace.sharedWorkspace();
			Object workspace = invoke(nsworkspaceCls, "sharedWorkspace", new Object[] {});
			//NSString fullPath = NSString.stringWith(path);
			Object fullPath = invoke(nsstringCls, "stringWith", new Object[] {
				path
			});
			if (fullPath != null) {
				// SWT also had a :
				// fullPath = workspace.fullPathForApplication(NSString.stringWith(name));
				// which might be handy someday, but for now, full path works

				//NSImage nsImage = workspace.iconForFile(fullPath);
				Object nsImage = invoke(workspace, "iconForFile", new Class[] {
					nsstringCls
				}, new Object[] {
					fullPath
				});
				if (nsImage != null) {
					//NSSize size = new NSSize();
					Object size = nssizeCls.newInstance();
					//size.width = size.height = imageWidthHeight;
					nssizeCls.getField("width").set(size, imageWidthHeight);
					nssizeCls.getField("height").set(size, imageWidthHeight);
					//nsImage.setSize(size);
					invoke(nsImage, "setSize", new Class[] {
						nssizeCls
					}, new Object[] {
						size
					});
					//nsImage.retain();
					invoke(nsImage, "retain");
					//Image image = Image.cocoa_new(Display.getCurrent(), SWT.BITMAP, nsImage);
					Image image = (Image) invoke(Image.class, null, "cocoa_new",
							new Class[] {
								Device.class,
								int.class,
								nsimageCls
							}, new Object[] {
								Display.getCurrent(),
								SWT.BITMAP,
								nsImage
							});
				return image;
				}
			}
		} catch (Throwable t) {
			Debug.printStackTrace(t);
		} finally {
			if (pool != null) {
				invoke(pool, "release");
			}
		}
		return null;
	}


	public static boolean isInitialized() {
		return initialized;
	}
}
