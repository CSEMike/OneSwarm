/*
 * Created on 8 juil. 2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.swt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.ui.swt.mainwindow.Initializer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.launcher.Launcher;

import edu.washington.cs.oneswarm.ui.gwt.RemoteAccessConfig;

/**
 * @author Olivier
 *
 */
public class Main
{
	private static final LogIDs LOGID						 = LogIDs.GUI;

	public static final String	PR_MULTI_INSTANCE = "MULTI_INSTANCE"; // values "true" or "false"

	StartServer								 startServer;

	// This method is called by other Main classes via reflection - must be kept public.
	public Main(String args[]) {
		try {
			// this should not be necessary, but since it's public let's play safe
			if (Launcher.checkAndLaunch(Main.class, args))
				return;

			// This *has* to be done first as it sets system properties that are read and cached by Java

			COConfigurationManager.preInitialise();

			String mi_str = System.getProperty(PR_MULTI_INSTANCE);

			boolean mi = mi_str != null && mi_str.equalsIgnoreCase("true");

			/**
			 * isdal mod, check for autostart flag and set nosplash
			 *
			 * check for configure flag as run configure script
			 */
			List<String> newargs = new LinkedList<String>();
			boolean autoStart = false;
			boolean configure = false;
			System.out.print("args: ");
			for (String s : args) {
				System.out.print(s + " ");
				if ("--autostart".equalsIgnoreCase(s)) {
					autoStart = true;
				} else if ("--configure".equalsIgnoreCase(s)) {
					configure = true;
				} else {
					newargs.add(s);
				}
			}
			System.out.println("");
			if (autoStart) {
				System.setProperty("nolaunch_startup", "");
				// and continue without the --autostart arg
				args = newargs.toArray(new String[newargs.size()]);
			}

			if (configure) {
				runConfigure();
				System.exit(0);
			}

			/**
			 * PIAMOD --
			 *
			 * Special case the multi instance code here -- by default, we launch with MULTI_INSTANCE since
			 * we wanted to coexist with Azureus. Special case is if we're launching with arguments since, on
			 * windows, that's how URL handlers are invoked, and Azureus uses the start server to transfer passed
			 * URI info to the running process.
			 *
			 * These days, we've just switched the port of the StartServer, so we don't need this anymore. and, we need
			 * to pass arguments to support links
			 */
			mi = false;

			startServer = new StartServer();

			boolean debugGUI = Boolean.getBoolean("debug");

			if (mi || debugGUI) {

				// create a MainWindow regardless to the server state

				AzureusCore core = AzureusCoreFactory.create();

				new Initializer(core, startServer, args);

				return;
			}

			if (processParams(args, startServer)) {

				AzureusCore core = AzureusCoreFactory.create();

				startServer.pollForConnections(core);

				new Initializer(core, startServer, args);

			}

		} catch (AzureusCoreException e) {

			Logger.log(new LogEvent(LOGID, "Start failed", e));
		}
	}

	private void runConfigure() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		try {
			COConfigurationManager.initialise();
			System.out.println("===OneSwarm configure===");
			System.out.println("Enable remote access? (y/n)");
			String line = in.readLine();
			if (line.equalsIgnoreCase("yes") || line.equalsIgnoreCase("y")) {
				System.out.println("Enter the new remote access user name:");
				String userName = in.readLine();
				Thread eraseThread = new Thread(new Runnable() {
					public void run() {
						try {
							while (true) {
								Thread.sleep(100);
								System.out.print("\b*");
							}
						} catch (InterruptedException e) {
						}
					}
				});
				eraseThread.start();
				String password;
				String password2;
				boolean passwordOk = true;
				do {
					System.out.println("Enter the new remote access password (min 8 characters):");
					password = in.readLine();
					System.out.println("Enter the password again: ");
					password2 = in.readLine();
					if (!password2.equals(password)) {
						passwordOk = false;
						System.out.println("password does not match");
					} else if (password.length() < 8) {
						passwordOk = false;
						System.out.println("password to short, min length is 8");
					} else {
						passwordOk = true;
					}
				} while (!passwordOk);
				eraseThread.interrupt();
				RemoteAccessConfig.saveRemoteAccessCredentials(userName, password);
				COConfigurationManager.setParameter("OSGWTUI.RemoteAccess", true);
				System.out.println("Remote access password saved");
			} else {
				COConfigurationManager.setParameter("OSGWTUI.RemoteAccess", false);
			}
			System.out.println("Enter tcp listen port ["
					+ COConfigurationManager.getIntParameter("TCP.Listen.Port") + "]:");
			String tcpPort = in.readLine();
			if (tcpPort.length() > 0) {
				COConfigurationManager.setParameter("TCP.Listen.Port",
						Integer.parseInt(tcpPort));
			}
			boolean pathOk = false;
			do {
				System.out.println("Enter default save location["
						+ COConfigurationManager.getDirectoryParameter("Default save path")
						+ "]:");
				String path = in.readLine();
				if (path.length() == 0) {
					pathOk = true;
				} else {
					File f = new File(path);
					if (f.isDirectory()) {
						File testFile = new File(f, "testing_if_write_is_permitted.txt");
						testFile.createNewFile();
						if (testFile.isFile()) {
							testFile.delete();
							pathOk = true;
						} else {
							System.out.println("directory not writable");
						}
					} else {
						System.out.println("directory does not exit, create? (y/n)");
						line = in.readLine();
						if (line.equalsIgnoreCase("yes") || line.equalsIgnoreCase("y")) {
							boolean ok = f.mkdirs();
							if (ok) {
								pathOk = true;
							} else {
								System.out.println("unable to create dir: '" + path + "'");
							}
						}
					}
				}
				if (pathOk) {
					COConfigurationManager.setParameter("Default save path", path);
				}
			} while (!pathOk);
			COConfigurationManager.save();
			System.out.println("Configuration done, the rest can be done using the webpage");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 * @return whether to init the core
	 */
	public static boolean processParams(String[] args, StartServer startServer) {
		boolean closedown = false;

		boolean another_instance = startServer.getState() != StartServer.STATE_LISTENING;

		/*  if another instance is running then set the property which is checked during
		 *	class instantiation by various stuff to to avoid pulling in too much state
		 *	from the already running instance
		 */
		if (another_instance)

			System.setProperty("transitory.startup", "1");

		// WATCH OUT FOR LOGGING HERE - we don't want to use Logger if this is a secondary instance as
		// it initialised TOO MUCH of AZ core

		for (int i = 0; i < args.length; i++) {

			String arg = args[i];

			if (arg.equalsIgnoreCase("--closedown")) {

				closedown = true;

				break;
			}
			// Sometimes Windows use filename in 8.3 form and cannot
			// match .torrent extension. To solve this, canonical path
			// is used to get back the long form

			String filename = arg;

			if (filename.toUpperCase().startsWith("HTTP:")
					|| filename.toUpperCase().startsWith("HTTPS:")
					|| filename.toUpperCase().startsWith("MAGNET:")) {

				if (!another_instance) {

					Logger.log(new LogEvent(LOGID, "Main::main: args[" + i
							+ "] handling as a URI: " + filename));
				}

				continue; //URIs cannot be checked as a .torrent file
			}

			try {
				File file = new File(filename);

				if (!file.exists()) {

					throw (new Exception("File '" + file + "' not found"));
				}

				args[i] = file.getCanonicalPath();

				// don't use logger if we're not the main instance as we don't want all
				// the associated core initialisation + debug file moving...

				if ((!another_instance) && Logger.isEnabled()) {

					Logger.log(new LogEvent(LOGID, "Main::main: args[" + i
							+ "] exists = " + new File(filename).exists()));
				}
			} catch (Throwable e) {

				if (another_instance) {

					e.printStackTrace();

				} else {

					Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
							"Failed to access torrent file '" + filename
									+ "'. Ensure sufficient temporary "
									+ "file space available (check browser cache usage)."));
				}
			}
		}

		if (another_instance) { //looks like there's already a process listening on 127.0.0.1:6885
			//attempt to pass args to existing instance

			StartSocket ss = new StartSocket(args);

			if (!ss.sendArgs()) { //arg passing attempt failed, so start core anyway
				another_instance = false;
				String msg = "There appears to be another program process already listening on socket [127.0.0.1: 6885].\nLoading of torrents via command line parameter will fail until this is fixed.";
				System.out.println(msg);
				Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_WARNING, msg));
			}
		}

		if (!another_instance) {

			if (closedown) {
				// closedown request and no instance running
				return false;
			}

			return true;
		}
		return false;
	}

	public static void main(String args[]) {
		System.out.println("OneSwarm custom main");

		if (Launcher.checkAndLaunch(Main.class, args))
			return;
		//Debug.dumpThreads("Entry threads");

		//Debug.dumpSystemProperties();

		if (System.getProperty("ui.temp") == null) {
			System.setProperty("ui.temp", "az2");
		}

		new Main(args);
	}
}
