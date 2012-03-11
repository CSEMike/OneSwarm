/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials  * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *  * Contributors:
 *     IBM Corporation - initial API and implementation
 * 		 Aelitis - Adaptation for Azureus
 *******************************************************************************/
package org.gudy.azureus2.ui.swt.osx;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.platform.macosx.access.jnilib.OSXAccess;
import org.gudy.azureus2.ui.swt.UIExitUtilsSWT;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.nat.NatTestWindow;
import org.gudy.azureus2.ui.swt.speedtest.SpeedTestWizard;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;

public class CarbonUIEnhancer
{
	// Most of these constants come from SWT OS.java.  Could have used reflection
	// to get them, but I assume they are truly constant (will never change)
	private static final int noErr = 0;

	private static final int eventNotHandledErr = -9874;

	private static final int kEventWindowToolbarSwitchMode = 150;

	private static final int kWindowToolbarButtonAttribute = (1 << 6);

	private static final int kEventAppleEvent = 1;

	private static final int kEventProcessCommand = 1;

	private static final int kCFAllocatorDefault = 0;

	private static final int kMenuItemAttrSeparator = 64;

	private static final int kCFURLPOSIXPathStyle = 0;

	private static final int kEventClassWindow = ('w' << 24) + ('i' << 16)
			+ ('n' << 8) + 'd';

	private static final int kAEQuitApplication = ('q' << 24) + ('u' << 16)
			+ ('i' << 8) + 't';

	private static final int kEventClassAppleEvent = ('e' << 24) + ('p' << 16)
			+ ('p' << 8) + 'c';

	private static final int kEventParamDirectObject = ('-' << 24) + ('-' << 16)
			+ ('-' << 8) + '-';

	private static final int kEventClassCommand = ('c' << 24) + ('m' << 16)
			+ ('d' << 8) + 's';

	private static final int kEventParamAEEventID = ('e' << 24) + ('v' << 16)
			+ ('t' << 8) + 'i';

	private static final int typeHICommand = ('h' << 24) + ('c' << 16)
			+ ('m' << 8) + 'd';

	private static final int typeFSRef = ('f' << 24) + ('s' << 16) + ('r' << 8)
			+ 'f';

	private static final int typeWindowRef = ('w' << 24) + ('i' << 16)
			+ ('n' << 8) + 'd';

	private static final int typeType = ('t' << 24) + ('y' << 16) + ('p' << 8)
			+ 'e';

	private static final int kHICommandPreferences = ('p' << 24) + ('r' << 16)
			+ ('e' << 8) + 'f';

	private static final int kHICommandAbout = ('a' << 24) + ('b' << 16)
			+ ('o' << 8) + 'u';

	private static final int kHICommandServices = ('s' << 24) + ('e' << 16)
			+ ('r' << 8) + 'v';

	private static final int kHICommandWizard = ('a' << 24) + ('z' << 16)
			+ ('c' << 8) + 'n';

	private static final int kHICommandNatTest = ('a' << 24) + ('z' << 16)
			+ ('n' << 8) + 't';

	private static final int kHICommandSpeedTest = ('a' << 24) + ('z' << 16)
			+ ('s' << 8) + 't';

	private static final int kHICommandRestart = ('a' << 24) + ('z' << 16)
			+ ('r' << 8) + 's';

	private static final int typeAEList = ('l' << 24) + ('i' << 16) + ('s' << 8)
			+ 't';

	private static final int kCoreEventClass = ('a' << 24) + ('e' << 16)
			+ ('v' << 8) + 't';

	private static final int kAEOpenDocuments = ('o' << 24) + ('d' << 16)
			+ ('o' << 8) + 'c';

	private static final int kAEReopenApplication = ('r' << 24) + ('a' << 16)
			+ ('p' << 8) + 'p';

	private static final int kAEOpenContents = ('o' << 24) + ('c' << 16)
			+ ('o' << 8) + 'n';

	private static final int kURLEventClass = ('G' << 24) + ('U' << 16)
			+ ('R' << 8) + 'L';

	private static final int typeText = ('T' << 24) + ('E' << 16) + ('X' << 8)
			+ 'T';

	private static String fgAboutActionName;

	private static String fgWizardActionName;

	private static String fgNatTestActionName;

	private static String fgRestartActionName;

	private static String fgSpeedTestActionName;

	private static int memmove_type = 0;

	/**
	 * KN: Some of the menu items have been removed for the Vuze and Vuze Advanced UI's;
	 * the classic UI still retains all its menu items as before.  Follow this flag in the code
	 * to see which menu items are effected.
	 */
	private boolean isAZ3 = "az3".equalsIgnoreCase(COConfigurationManager.getStringParameter("ui"));

	private static Class<?> claCallback;

	private static Constructor<?> constCallback3;

	private static Method mCallback_getAddress;

	private static Method mCallback_dispose;

	private static Class<?> claOS;

	private static Class<?> claHICommand;

	private static Class<?> claCFRange;

	private static Class<?> claAEDesc;

	private static Class<?> claEventRecord;

	//public static final int BOUNCE_SINGLE = NSApplication.UserAttentionRequestInformational;

	//public static final int BOUNCE_CONTINUOUS = NSApplication.UserAttentionRequestCritical;

	static {
		try {
			claCallback = Class.forName("org.eclipse.swt.internal.Callback");
			//public Callback (Object object, String method, int argCount) {
			constCallback3 = claCallback.getConstructor(new Class[] {
				Object.class,
				String.class,
				int.class,
			});
			// public int /*long*/ getAddress () {
			mCallback_getAddress = claCallback.getMethod("getAddress", new Class[] {});
			// public void dispose () {
			mCallback_dispose = claCallback.getMethod("dispose", new Class[] {});

			claOS = Class.forName("org.eclipse.swt.internal.carbon.OS");

			claHICommand = Class.forName("org.eclipse.swt.internal.carbon.HICommand");

			claCFRange = Class.forName("org.eclipse.swt.internal.carbon.CFRange");

			claAEDesc = Class.forName("org.eclipse.swt.internal.carbon.AEDesc");

			claEventRecord = Class.forName("org.eclipse.swt.internal.carbon.EventRecord");
		} catch (Exception e) {
		}

	}

	public CarbonUIEnhancer() {
		if (fgAboutActionName == null) {
			fgAboutActionName = MessageText.getString("MainWindow.menu.help.about").replaceAll(
					"&", "");
		}

		if (false == isAZ3) {
			if (fgWizardActionName == null) {
				fgWizardActionName = MessageText.getString(
						"MainWindow.menu.file.configure").replaceAll("&", "");
			}
			if (fgNatTestActionName == null) {
				fgNatTestActionName = MessageText.getString(
						"MainWindow.menu.tools.nattest").replaceAll("&", "");
			}

			if (fgSpeedTestActionName == null) {
				fgSpeedTestActionName = MessageText.getString(
						"MainWindow.menu.tools.speedtest").replaceAll("&", "");
			}
		}

		if (fgRestartActionName == null) {
			fgRestartActionName = MessageText.getString(
					"MainWindow.menu.file.restart").replaceAll("&", "");
		}
		earlyStartup();
		registerTorrentFile();
	}

	public static void registerToolbarToggle(Shell shell) {
		try {
			final Object toolbarToggleCB = constCallback3.newInstance(
					CarbonUIEnhancer.class, "toolbarToggle", 3);
			int toolbarToggle = ((Number) mCallback_getAddress.invoke(
					toolbarToggleCB, new Object[] {})).intValue();
			if (toolbarToggle == 0) {
				Debug.out("OSX: Could not find callback 'toolbarToggle'");
				mCallback_dispose.invoke(toolbarToggleCB, new Object[] {});
				return;
			}

			shell.getDisplay().disposeExec(new Runnable() {
				public void run() {
					try {
						mCallback_dispose.invoke(toolbarToggleCB, new Object[] {});
					} catch (Exception e) {
					}
				}
			});

			//	 add the button to the window trim
			Object oHandle = shell.getClass().getField("handle").get(shell);
			int windowHandle = ((Number) invoke(claOS, null, "GetControlOwner",
					new Object[] {
						oHandle
					})).intValue();
			invoke(claOS, null, "ChangeWindowAttributes", new Object[] {
				windowHandle,
				kWindowToolbarButtonAttribute,
				0
			});

			int[] mask = new int[] {
				kEventClassWindow,
				kEventWindowToolbarSwitchMode
			};
			// register the handler with the OS
			int applicationEventTarget = ((Number) invoke(claOS, null,
					"GetApplicationEventTarget", new Object[] {})).intValue();
			// int InstallEventHandler(int inTarget, int inHandler, int inNumTypes, int[] inList, int inUserData, int[] outRef);
			invoke(claOS, null, "InstallEventHandler", new Class[] {
				int.class,
				int.class,
				int.class,
				int[].class,
				int.class,
				int[].class
			}, new Object[] {
				applicationEventTarget,
				toolbarToggle,
				mask.length / 2,
				mask,
				0,
				null
			});
		} catch (Throwable e) {
			Debug.out("RegisterToolbarToggle failed", e);
		}
	}

	private void registerTorrentFile() {
		try {
			int result;

			Object clickDockIconCallback = constCallback3.newInstance(
					CarbonUIEnhancer.class, "clickDockIcon", 3);
			int clickDocIcon = ((Number) mCallback_getAddress.invoke(
					clickDockIconCallback, new Object[] {})).intValue();
			if (clickDocIcon == 0) {
				mCallback_dispose.invoke(clickDockIconCallback, new Object[] {});
			} else {
				result = ((Number) invoke(claOS, null, "AEInstallEventHandler",
						new Object[] {
							kCoreEventClass,
							kAEReopenApplication,
							clickDocIcon,
							(int) 0,
							false
						})).intValue();

				if (result != noErr) {
					Debug.out("OSX: Could Install ReopenApplication Event Handler. Error: "
							+ result);
				}
			}

			Object openContentsCallback = constCallback3.newInstance(
					CarbonUIEnhancer.class, "openContents", 3);
			int openContents = ((Number) mCallback_getAddress.invoke(
					openContentsCallback, new Object[] {})).intValue();
			if (openContents == 0) {
				mCallback_dispose.invoke(openContentsCallback, new Object[] {});
			} else {
				result = ((Number) invoke(claOS, null, "AEInstallEventHandler",
						new Object[] {
							kCoreEventClass,
							kAEOpenContents,
							openContents,
							(int) 0,
							false
						})).intValue();

				if (result != noErr) {
					Debug.out("OSX: Could Install OpenContents Event Handler. Error: "
							+ result);
				}
			}

			Object openDocCallback = constCallback3.newInstance(
					CarbonUIEnhancer.class, "openDocProc", 3);
			int openDocProc = ((Number) mCallback_getAddress.invoke(openDocCallback,
					new Object[] {})).intValue();
			if (openDocProc == 0) {
				Debug.out("OSX: Could not find Callback 'openDocProc'");
				mCallback_dispose.invoke(openDocCallback, new Object[] {});
				return;
			}

			result = ((Number) invoke(claOS, null, "AEInstallEventHandler",
					new Object[] {
						kCoreEventClass,
						kAEOpenDocuments,
						openDocProc,
						(int) 0,
						false
					})).intValue();

			if (result != noErr) {
				Debug.out("OSX: Could not Install OpenDocs Event Handler. Error: "
						+ result);
				return;
			}

			result = ((Number) invoke(claOS, null, "AEInstallEventHandler",
					new Object[] {
						kURLEventClass,
						kURLEventClass,
						openDocProc,
						(int) 0,
						false
					})).intValue();
			if (result != noErr) {
				Debug.out("OSX: Could not Install URLEventClass Event Handler. Error: "
						+ result);
				return;
			}

			///

			Object quitAppCallback = constCallback3.newInstance(
					CarbonUIEnhancer.class, "quitAppProc", 3);
			int quitAppProc = ((Number) mCallback_getAddress.invoke(quitAppCallback,
					new Object[] {})).intValue();
			if (quitAppProc == 0) {
				Debug.out("OSX: Could not find Callback 'quitApp'");
				mCallback_dispose.invoke(quitAppCallback, new Object[] {});
			} else {
				result = ((Number) invoke(claOS, null, "AEInstallEventHandler",
						new Object[] {
							kCoreEventClass,
							kAEQuitApplication,
							quitAppProc,
							(int) 0,
							false
						})).intValue();
				if (result != noErr) {
					Debug.out("OSX: Could not install QuitApplication Event Handler. Error: "
							+ result);
				}
			}

			///

			int appTarget = ((Number) invoke(claOS, null,
					"GetApplicationEventTarget", new Object[] {})).intValue();
			Object appleEventCallback = constCallback3.newInstance(this,
					"appleEventProc", 3);
			int appleEventProc = ((Number) mCallback_getAddress.invoke(
					appleEventCallback, new Object[] {})).intValue();
			int[] mask3 = new int[] {
				kEventClassAppleEvent,
				kEventAppleEvent,
				kURLEventClass,
				kAEReopenApplication,
				kAEOpenContents,
			};
			result = ((Number) invoke(claOS, null, "InstallEventHandler",
					new Class[] {
						int.class,
						int.class,
						int.class,
						int[].class,
						int.class,
						int[].class
					}, new Object[] {
						appTarget,
						appleEventProc,
						mask3.length / 2,
						mask3,
						0,
						null
					})).intValue();
			if (result != noErr) {
				Debug.out("OSX: Could Install Event Handler. Error: " + result);
				return;
			}
		} catch (Throwable e) {
			Debug.out("registerTorrentFile failed", e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	public void earlyStartup() {
		final Display display = Display.getDefault();
		display.syncExec(new AERunnable() {
			public void runSupport() {
				hookApplicationMenu(display);
			}
		});
	}

	/**
	* See Apple Technical Q&A 1079 (http://developer.apple.com/qa/qa2001/qa1079.html)<br />
	* Also http://developer.apple.com/documentation/Carbon/Reference/Menu_Manager/menu_mgr_ref/function_group_10.html
	*/
	public void hookApplicationMenu(final Display display) {
		try {
			final Object commandCallback = constCallback3.newInstance(
					CarbonUIEnhancer.class, "commandProc", 3); //$NON-NLS-1$
			int commandProc = ((Number) mCallback_getAddress.invoke(commandCallback,
					new Object[] {})).intValue();
			if (commandProc == 0) {
				mCallback_dispose.invoke(commandCallback, new Object[] {});
				return; // give up
			}

			// Install event handler for commands
			int[] mask = new int[] {
				kEventClassCommand,
				kEventProcessCommand
			};
			int appTarget = ((Number) invoke(claOS, null,
					"GetApplicationEventTarget", new Object[] {})).intValue();
			invoke(claOS, null, "InstallEventHandler", new Class[] {
				int.class,
				int.class,
				int.class,
				int[].class,
				int.class,
				int[].class
			}, new Object[] {
				appTarget,
				commandProc,
				mask.length / 2,
				mask,
				0,
				null
			});

			// create About menu command
			int[] outMenu = new int[1];
			short[] outIndex = new short[1];
			// int GetIndMenuItemWithCommandID(int mHandle, int commandId, int index, int[] outMenu, short[] outIndex);
			int ind = ((Number) invoke(claOS, null, "GetIndMenuItemWithCommandID",
					new Class[] {
						int.class,
						int.class,
						int.class,
						int[].class,
						short[].class
					}, new Object[] {
						0,
						kHICommandPreferences,
						1,
						outMenu,
						outIndex
					})).intValue();
			if (ind == noErr && outMenu[0] != 0) {
				int menu = outMenu[0];

				int l = fgAboutActionName.length();
				char buffer[] = new char[l];
				fgAboutActionName.getChars(0, l, buffer, 0);
				int str = CFStringCreateWithCharacters(kCFAllocatorDefault, buffer, l);
				InsertMenuItemTextWithCFString(menu, str, (short) 0, 0, kHICommandAbout);
				invoke(claOS, null, "CFRelease", new Object[] {
					str
				});
				// add separator between About & Preferences
				InsertMenuItemTextWithCFString(menu, 0, (short) 1,
						kMenuItemAttrSeparator, 0);

				// enable pref menu
				invoke(claOS, null, "EnableMenuCommand", new Object[] {
					menu,
					kHICommandPreferences
				});
				// disable services menu
				invoke(claOS, null, "DisableMenuCommand", new Object[] {
					menu,
					kHICommandServices
				});

				if (!isAZ3) {
					// wizard menu
					l = fgWizardActionName.length();
					buffer = new char[l];
					fgWizardActionName.getChars(0, l, buffer, 0);
					str = CFStringCreateWithCharacters(kCFAllocatorDefault, buffer, l);
					InsertMenuItemTextWithCFString(menu, str, (short) 3, 0,
							kHICommandWizard);
					invoke(claOS, null, "CFRelease", new Object[] {
						str
					});

					// NAT test menu
					l = fgNatTestActionName.length();
					buffer = new char[l];
					fgNatTestActionName.getChars(0, l, buffer, 0);
					str = CFStringCreateWithCharacters(kCFAllocatorDefault, buffer, l);
					InsertMenuItemTextWithCFString(menu, str, (short) 4, 0,
							kHICommandNatTest);
					invoke(claOS, null, "CFRelease", new Object[] {
						str
					});

					//SpeedTest
					l = fgSpeedTestActionName.length();
					buffer = new char[l];
					fgSpeedTestActionName.getChars(0, l, buffer, 0);
					str = CFStringCreateWithCharacters(kCFAllocatorDefault, buffer, l);
					InsertMenuItemTextWithCFString(menu, str, (short) 5, 0,
							kHICommandSpeedTest);
					invoke(claOS, null, "CFRelease", new Object[] {
						str
					});
				}

				InsertMenuItemTextWithCFString(menu, 0, (short) 6,
						kMenuItemAttrSeparator, 0);

				// restart menu
				l = fgRestartActionName.length();
				buffer = new char[l];
				fgRestartActionName.getChars(0, l, buffer, 0);
				str = CFStringCreateWithCharacters(kCFAllocatorDefault, buffer, l);
				InsertMenuItemTextWithCFString(menu, str, (short) 7, 0,
						kHICommandRestart);
				invoke(claOS, null, "CFRelease", new Object[] {
					str
				});

				InsertMenuItemTextWithCFString(menu, 0, (short) 8,
						kMenuItemAttrSeparator, 0);
			}

			// schedule disposal of callback object
			display.disposeExec(new AERunnable() {
				public void runSupport() {
					try {
						mCallback_dispose.invoke(commandCallback, new Object[] {});
					} catch (Throwable e) {
					}
					//               stopSidekick();
				}
			});
		} catch (Throwable e) {
			Debug.out("Failed hookApplicatioMenu", e);
		}
	}

	private void InsertMenuItemTextWithCFString(int mHandle, int sHandle,
			short index, int attributes, int commandID) {
		//OS.InsertMenuItemTextWithCFString(mHandle, sHandle, index, attributes, commandID);
		invoke(claOS, null, "InsertMenuItemTextWithCFString", new Class[] {
			int.class,
			int.class,
			short.class,
			int.class,
			int.class
		}, new Object[] {
			mHandle,
			sHandle,
			index,
			attributes,
			commandID
		});
	}

	private int CFStringCreateWithCharacters(int alloc, char[] buffer,
			int numChars) {
		//return OS.CFStringCreateWithCharacters(alloc, buffer, numChars);
		return ((Number) invoke(claOS, null, "CFStringCreateWithCharacters",
				new Object[] {
					alloc,
					buffer,
					numChars
				})).intValue();
	}

	int appleEventProc(int nextHandler, int theEvent, int userData) {
		try {
			int eventClass = ((Number) invoke(claOS, null, "GetEventClass",
					new Object[] {
						theEvent
					})).intValue();
			//int eventKind = OS.GetEventKind(theEvent);

			//System.out.println("appleEventProc " + OSXtoString(eventClass) + ";"
			//		+ OS.GetEventKind(theEvent) + ";" + OSXtoString(theEvent) + ";"
			//		+ OSXtoString(userData));

			// Process the odoc event
			if (eventClass == kEventClassAppleEvent) {
				int[] aeEventID = new int[1];
				// int GetEventParameter(int inEvent, int inName, int inDesiredType, 
				// int[] outActualType, int inBufferSize, int[] outActualSize, int[] outData);

				int ret = ((Number) invoke(claOS, null, "GetEventParameter",
						new Class[] {
							int.class,
							int.class,
							int.class,
							int[].class,
							int.class,
							int[].class,
							int[].class
						}, new Object[] {
							theEvent,
							kEventParamAEEventID,
							typeType,
							null,
							4,
							null,
							aeEventID
						})).intValue();

				if (ret != noErr) {
					return eventNotHandledErr;
				}
				//System.out.println("EventID = " + OSXtoString(aeEventID[0]));
				if (aeEventID[0] != kAEOpenDocuments && aeEventID[0] != kURLEventClass
						&& aeEventID[0] != kAEReopenApplication
						&& aeEventID[0] != kAEOpenContents
						&& aeEventID[0] != kAEQuitApplication) {
					return eventNotHandledErr;
				}

				// Handle Event
				Object eventRecord = claEventRecord.newInstance();
				invoke(claOS, null, "ConvertEventRefToEventRecord", new Class[] {
					int.class,
					claEventRecord
				}, new Object[] {
					theEvent,
					eventRecord
				});
				invoke(claOS, null, "AEProcessAppleEvent", new Object[] {
					eventRecord
				});

				// Tell Mac we are handling this event
				return noErr;
			}

		} catch (Throwable e) {
			Debug.out(e);
		}
		return eventNotHandledErr;
	}

	/*
	private static String OSXtoString(int i) {
		char[] c = new char[4];
		c[0] = (char) ((i >> 24) & 0xff);
		c[1] = (char) ((i >> 16) & 0xff);
		c[2] = (char) ((i >> 8) & 0xff);
		c[3] = (char) (i & 0xff);
		return new String(c);
	}
	*/

	private static void memmove(byte[] dest, int src, int size) {
		switch (memmove_type) {
			case 0:
				try {
					OSXAccess.memmove(dest, src, size);
					memmove_type = 0;
					return;
				} catch (Throwable e) {
				}
				// FALL THROUGH

			case 1:
				try {
					Class<?> cMemMove = Class.forName("org.eclipse.swt.internal.carbon.OS");

					Method method = cMemMove.getMethod("memmove", new Class[] {
						byte[].class,
						Integer.TYPE,
						Integer.TYPE
					});

					method.invoke(null, new Object[] {
						dest,
						new Integer(src),
						new Integer(size)
					});
					memmove_type = 1;
					return;
				} catch (Throwable e) {
				}

				// FALL THROUGH
			case 2:
				try {
					Class<?> cMemMove = Class.forName("org.eclipse.swt.internal.carbon.OS");

					Method method = cMemMove.getMethod("memcpy", new Class[] {
						byte[].class,
						Integer.TYPE,
						Integer.TYPE
					});

					method.invoke(null, new Object[] {
						dest,
						new Integer(src),
						new Integer(size)
					});

					memmove_type = 2;
					return;
				} catch (Throwable e) {
				}

				// FALL THROUGH

			default:
				break;
		}

		memmove_type = 3;
	}

	final static int commandProc(int nextHandler, int theEvent, int userData) {
		try {
			int kind = ((Number) invoke(claOS, null, "GetEventKind", new Object[] {
				theEvent
			})).intValue();
			if (kind == kEventProcessCommand) {
				Object command = claHICommand.newInstance();
				// int GetEventParameter(int inEvent, int inName, int inDesiredType, 
				// int[] outActualType, int inBufferSize, int[] outActualSize, HICommand outData);
				invoke(claOS, null, "GetEventParameter", new Class[] {
					int.class,
					int.class,
					int.class,
					int[].class,
					int.class,
					int[].class,
					claHICommand
				}, new Object[] {
					theEvent,
					kEventParamDirectObject,
					typeHICommand,
					null,
					claHICommand.getField("sizeof").getInt(command),
					null,
					command
				});
				int commandID = claHICommand.getField("commandID").getInt(command);
				switch (commandID) {
					case kHICommandPreferences: {
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.openView(UIFunctions.VIEW_CONFIG, null);
						}
						return noErr;
					}
					case kHICommandAbout:
						AboutWindow.show();
						return noErr;
					case kHICommandRestart: {
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.dispose(true, false);
						}
						return noErr;
					}
					case kHICommandWizard:
						new ConfigureWizard(false,ConfigureWizard.WIZARD_MODE_FULL);
						return noErr;
					case kHICommandNatTest:
						new NatTestWindow();
						return noErr;
					case kHICommandSpeedTest:
						new SpeedTestWizard();
						return noErr;

					case kAEQuitApplication:
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.dispose(false, false);
							return noErr;
						} else {
							UIExitUtilsSWT.setSkipCloseCheck(true);
						}
					default:
						break;
				}
			}
		} catch (Throwable t) {
			Debug.out(t);
		}
		return eventNotHandledErr;
	}

	final static int quitAppProc(int theAppleEvent, int reply, int handlerRefcon) {
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.dispose(false, false);
		} else {
			UIExitUtilsSWT.setSkipCloseCheck(true);
			Display.getDefault().dispose();
		}
		return noErr;
	}

	final static int openDocProc(int theAppleEvent, int reply, int handlerRefcon) {
		try {
			Object aeDesc = claAEDesc.newInstance();
			Object eventRecord = claEventRecord.newInstance();
			invoke(claOS, null, "ConvertEventRefToEventRecord", new Class[] {
				int.class,
				claEventRecord
			}, new Object[] {
				theAppleEvent,
				eventRecord
			});
			try {
				int result = OSXAccess.AEGetParamDesc(theAppleEvent,
						kEventParamDirectObject, typeAEList, aeDesc);
				if (result != noErr) {
					Debug.out("OSX: Could call AEGetParamDesc. Error: " + result);
					return noErr;
				}
			} catch (java.lang.UnsatisfiedLinkError e) {
				Debug.out("OSX: AEGetParamDesc not available.  Can't open sent file");
				return noErr;
			}

			int[] count = new int[1];
			invoke(claOS, null, "AECountItems", new Class[] {
				claAEDesc,
				int[].class
			}, new Object[] {
				aeDesc,
				count
			});
			//System.out.println("COUNT: " + count[0]);
			if (count[0] > 0) {
				final String[] fileNames = new String[count[0]];
				int maximumSize = 80; // size of FSRef
				int dataPtr = ((Number) invoke(claOS, null, "NewPtr", new Object[] {
					maximumSize
				})).intValue();
				int[] aeKeyword = new int[1];
				int[] typeCode = new int[1];
				int[] actualSize = new int[1];
				for (int i = 0; i < count[0]; i++) {
					try {
						// int AEGetNthPtr(AEDesc theAEDescList, int index, int desiredType, 
						// int[] theAEKeyword, int[] typeCode, int dataPtr, int maximumSize, int[] actualSize);
						Class<?>[] sigAEGetNthPtr = new Class[] {
							claAEDesc,
							int.class,
							int.class,
							int[].class,
							int[].class,
							int.class,
							int.class,
							int[].class
						};
						int ret = ((Number) invoke(claOS, null, "AEGetNthPtr",
								sigAEGetNthPtr, new Object[] {
									aeDesc,
									i + 1,
									typeFSRef,
									aeKeyword,
									typeCode,
									dataPtr,
									maximumSize,
									actualSize
								})).intValue();
						if (ret == noErr) {
							byte[] fsRef = new byte[actualSize[0]];
							memmove(fsRef, dataPtr, actualSize[0]);
							int dirUrl = ((Number) invoke(claOS, null,
									"CFURLCreateFromFSRef", new Object[] {
										kCFAllocatorDefault,
										fsRef
									})).intValue();
							int dirString = ((Number) invoke(claOS, null,
									"CFURLCopyFileSystemPath", new Object[] {
										dirUrl,
										kCFURLPOSIXPathStyle
									})).intValue();
							int length = ((Number) invoke(claOS, null, "CFStringGetLength",
									new Object[] {
										dirString
									})).intValue();
							char[] buffer = new char[length];
							Object range = claCFRange.newInstance();
							claCFRange.getField("length").setInt(range, length);
							invoke(claOS, null, "CFStringGetCharacters", new Class[] {
								int.class,
								claCFRange,
								char[].class
							}, new Object[] {
								dirString,
								range,
								buffer
							});
							fileNames[i] = new String(buffer);
							invoke(claOS, null, "CFRelease", new Object[] {
								dirString
							});
							invoke(claOS, null, "CFRelease", new Object[] {
								dirUrl
							});
						} else {
							ret = ((Number) invoke(claOS, null, "AEGetNthPtr",
									sigAEGetNthPtr, new Object[] {
										aeDesc,
										i + 1,
										typeText,
										aeKeyword,
										typeCode,
										dataPtr,
										2048,
										actualSize
									})).intValue();

							if (ret == noErr) {
								byte[] urlRef = new byte[actualSize[0]];
								memmove(urlRef, dataPtr, actualSize[0]);
								fileNames[i] = new String(urlRef);
							}
						}
					} catch (Throwable t) {
						Debug.out(t);
					}
					//System.out.println(fileNames[i]);
				}

				AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
					public void azureusCoreRunning(AzureusCore core) {
						TorrentOpener.openTorrents(fileNames);
					}
				});
			}

			return noErr;
		} catch (Throwable e) {
			Debug.out(e);
		}
		return eventNotHandledErr;
	}

	final static int clickDockIcon(int nextHandler, int theEvent, int userData) {
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.bringToFront();
			return noErr;
		}
		return eventNotHandledErr;
	}

	final static int openContents(int nextHandler, int theEvent, int userData) {
		Debug.out("openDocContents");
		return noErr;
	}

	final static int toolbarToggle(int nextHandler, int theEvent, int userData) {
		int eventKind = ((Number) invoke(claOS, null, "GetEventKind", new Object[] {
			theEvent
		})).intValue();
		if (eventKind != kEventWindowToolbarSwitchMode) {
			return eventNotHandledErr;
		}

		int[] theWindow = new int[1];
		//int GetEventParameter(int inEvent, int inName, int inDesiredType, 
		// int[] outActualType, int inBufferSize, int[] outActualSize, int[] outData);
		invoke(claOS, null, "GetEventParameter", new Class[] {
			int.class,
			int.class,
			int.class,
			int[].class,
			int.class,
			int[].class,
			int[].class
		}, new Object[] {
			theEvent,
			kEventParamDirectObject,
			typeWindowRef,
			null,
			4,
			null,
			theWindow
		});

		int[] theRoot = new int[1];
		invoke(claOS, null, "GetRootControl", new Object[] {
			theWindow[0],
			theRoot
		});
		final Widget widget = Display.getCurrent().findWidget(theRoot[0]);

		if (!(widget instanceof Shell)) {
			return eventNotHandledErr;
		}
		final Shell shellAffected = (Shell) widget;

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
				event.display = widget.getDisplay();
				event.widget = widget;
				shellAffected.notifyListeners(type, event);

				shellAffected.setData("OSX.ToolBarToggle", new Long(
						type == SWT.Collapse ? 1 : 0));
			}
		});

		return noErr;
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
			String methodName, Class<?>[] signature, Object[] args) {
		try {
			Method method = clazz.getDeclaredMethod(methodName, signature);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}