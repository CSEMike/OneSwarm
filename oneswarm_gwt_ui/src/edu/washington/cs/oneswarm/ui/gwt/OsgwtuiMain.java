package edu.washington.cs.oneswarm.ui.gwt;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginException;
import org.gudy.azureus2.plugins.PluginInterface;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.Credential;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.servlet.GzipFilter;
import org.mortbay.thread.BoundedThreadPool;
import org.mortbay.thread.QueuedThreadPool;

import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager.Sha1CalcListener;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager.Sha1HashJobListener;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager.Sha1Result;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager.CancellationListener;
import edu.washington.cs.oneswarm.ui.gwt.server.community.CommunityServerManager;
import edu.washington.cs.oneswarm.ui.gwt.server.handlers.MultiHandler;

public class OsgwtuiMain implements Plugin {

	public static final String LOCALHOST = "127.0.0.1";

	private Server authenticatedServer;
	private CoreInterface coreInterface;

	private PluginInterface pluginInterface;

	private RemoteAccessForward remoteAccessForward = null;

	private Server server;

	private synchronized void disableRemoteAccess() {
		if (remoteAccessForward != null) {
			System.out.println("disabling remote accesss");
			try {
				authenticatedServer.stop();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			remoteAccessForward.stop();
			remoteAccessForward = null;

		}
		coreInterface.setRemoteAccess(remoteAccessForward);
	}

	private static Logger logger = Logger.getLogger(OsgwtuiMain.class.getName());

	private synchronized void enableRemoteAccess() throws Exception {

		if (remoteAccessForward == null) {
			logger.fine("enabling remote access");
			Connector connector = new SelectChannelConnector();
			connector.setHost(LOCALHOST);
			connector.setPort(Constants.LOCAL_WEB_SERVER_PORT_AUTH);

			authenticatedServer = new Server();
			authenticatedServer.addConnector(connector);

			// sets the thread pool (just so it is deamon=true)
			QueuedThreadPool threadPool = new QueuedThreadPool();
			threadPool.setMinThreads(5);
			// threadPool.setMaxThreads(10);
			threadPool.setName("Auth Jetty thread pool");
			threadPool.setDaemon(true);
			authenticatedServer.setThreadPool(threadPool);

			Constraint constraint = new Constraint();
			constraint.setName(Constraint.__BASIC_AUTH);
			constraint.setRoles(new String[] { "remote_user" });
			constraint.setAuthenticate(true);

			ConstraintMapping cm = new ConstraintMapping();
			cm.setConstraint(constraint);
			cm.setPathSpec("/*");

			SecurityHandler securityHandler = new SecurityHandler();
			securityHandler.setUserRealm(new ExtraSaltHashUserRealm(RemoteAccessConfig.usesMD5Sha1Password(), "OneSwarm Remote", RemoteAccessConfig.REMOTE_ACCESS_FILE.getCanonicalPath()));
			securityHandler.setConstraintMappings(new ConstraintMapping[] { cm });

			ContextHandlerCollection contexts = new ContextHandlerCollection();

			authenticatedServer.setHandler(contexts);
			Context root = new Context(contexts, "/", Context.NO_SESSIONS);

			root.addFilter(new FilterHolder(new GzipFilter()), "/*", Handler.ALL);

			MultiHandler mh = new MultiHandler(coreInterface, true);

			if (System.getProperty("com.sun.management.jmxremote") != null) {
				RequestLogHandler requestLogHandler = new RequestLogHandler();

				NCSARequestLog requestLog = new NCSARequestLog("/tmp/jetty-yyyy_mm_dd.remoterequest.log");
				requestLog.setRetainDays(1);
				requestLog.setAppend(false);
				requestLog.setExtended(true);
				requestLog.setLogTimeZone("GMT");
				requestLogHandler.setRequestLog(requestLog);

				HandlerCollection handlers = new HandlerCollection();
				handlers.setHandlers(new Handler[] { mh, requestLogHandler });
				root.setHandler(handlers);
			} else {
				root.setHandler(mh);
			}

			root.addHandler(securityHandler);

			// make sure that the class loader can find all classes in the
			// osgwtui
			// plugin dir...
			root.setClassLoader(pluginInterface.getPluginClassLoader());

			authenticatedServer.start();

			remoteAccessForward = new RemoteAccessForward();
			remoteAccessForward.start();
			logger.fine("remote access enabled");
		}
		coreInterface.setRemoteAccess(remoteAccessForward);
	}

	public CoreInterface getCoreInterface() {
		return coreInterface;
	}

	public void initialize(PluginInterface pluginInterface) throws PluginException {
		this.coreInterface = new CoreInterface(pluginInterface);

		// make sure to unload in case of shutdown

		this.pluginInterface = pluginInterface;

		logger.fine("oneswarm ui plugin loaded");

		Connector connector = new SelectChannelConnector();
		connector.setHost(LOCALHOST);
		connector.setPort(Constants.LOCAL_WEB_SERVER_PORT);

		server = new Server();

		/**
		 * If we're running with jconsole support, start the MBean server
		 */
		if (System.getProperty("com.sun.management.jmxremote") != null) {
			connector.setStatsOn(true);

			logger.info("Starting managemenat bean");
			// MBeanServer mBeanServer =
			// ManagementFactory.getPlatformMBeanServer();
			// MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
			// server.getContainer().addEventListener(mBeanContainer);
			// mBeanContainer.start();
		}

		checkAutoStartRegistry();
		server.addConnector(connector);

		// sets the thread pool (just so it is deamon=true)
		BoundedThreadPool threadPool = new BoundedThreadPool();
		threadPool.setMinThreads(5);
		// threadPool.setMaxThreads(10);
		threadPool.setName("Jetty thread pool");
		threadPool.setDaemon(true);
		server.setThreadPool(threadPool);

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		server.setHandler(contexts);
		Context root = new Context(contexts, "/", Context.NO_SESSIONS);

		MultiHandler mh = new MultiHandler(coreInterface, false);

		if (System.getProperty("com.sun.management.jmxremote") != null) {
			RequestLogHandler requestLogHandler = new RequestLogHandler();

			NCSARequestLog requestLog = new NCSARequestLog("/tmp/jetty-yyyy_mm_dd.request.log");
			requestLog.setRetainDays(1);
			requestLog.setAppend(false);
			requestLog.setExtended(true);
			requestLog.setLogTimeZone("GMT");
			requestLogHandler.setRequestLog(requestLog);

			HandlerCollection handlers = new HandlerCollection();
			handlers.setHandlers(new Handler[] { mh, requestLogHandler });
			root.setHandler(handlers);
		} else {
			root.setHandler(mh);
		}

		// make sure that the class loader can find all classes in the osgwtui
		// plugin dir...
		root.setClassLoader(pluginInterface.getPluginClassLoader());

		root.setVirtualHosts(new String[] { LOCALHOST });

		try {
			server.start();
			if (isRemoteAccessAllowed()) {
				enableRemoteAccess();
			}
			installRemoteAccessPropertyListener();
			// Thread.sleep(10000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CommunityServerManager.get();

		// check to see if we can parse a planetlab-style experiment config file
		try {
			Class expConfigManagerClass = Class.forName("edu.washington.cs.oneswarm.planetlab.ExperimentConfigManager");
			if (expConfigManagerClass != null) {

				Method getMethod = expConfigManagerClass.getMethod("get");
				Object configManager = getMethod.invoke(null, new Object[] {});
				if (configManager != null) {
					logger.info("Got experimental manager");
					Method setCore = expConfigManagerClass.getMethod("setCore", new Class[] { CoreInterface.class });
					setCore.invoke(configManager, coreInterface);
					logger.info("Set core");
					Method startHeartbeats = expConfigManagerClass.getMethod("startHeartbeats");
					startHeartbeats.invoke(configManager);
					logger.info("startHeartbeats");
				} else {
					logger.info("configManager is null -- classes found but experimental mode not enabled");
				}
			}

		} catch (ClassNotFoundException e) {
			logger.info("PlanetLab classes not found -- not running in experimental mode.");
		} catch (Exception e) {
			System.err.println(e);
			logger.info("PlanetLab classes failed to load -- not running in experimental mode.");
		}

		// make sure community server refreshes whether we load the web UI or
		// not.
		CommunityServerManager.get();

		/*
		 * add the listener to the sha1 hasher manager
		 */
		Sha1HashManager.getInstance().addJobListener(new Sha1HashJobListener() {
			public Sha1CalcListener jobAdded(String name) {
				final int taskID = BackendTaskManager.get().createTask("Hashing: " + name, new CancellationListener() {
					public void cancelled(int inID) {
						Sha1HashManager.getInstance().stop();
					}
				});
				final BackendTask task = BackendTaskManager.get().getTask(taskID);
				task.setSummary("Calculating SHA1 and ED2K hashes of " + name);
				return new Sha1CalcListener() {
					public void progress(double fraction) {
						int percent = (int) Math.round(100 * fraction);
						task.setProgress(percent + "%");
					}

					public void errorOccured(Throwable cause) {
						BackendTaskManager.get().removeTask(taskID);
					}

					public void completed(Sha1Result result) {
						BackendTaskManager.get().removeTask(taskID);

					}
				};
			}
		});
	}

	private void checkAutoStartRegistry() {

		if (Constants.isWindows) {
			try {
				/*
				 * else, not first start sync up the setting with what is in the
				 * windows registry
				 */
				int handle = RegUtil.RegOpenKey(RegUtil.HKEY_CURRENT_USER, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run", RegUtil.KEY_ALL_ACCESS)[RegUtil.NATIVE_HANDLE];
				byte[] value = RegUtil.RegQueryValueEx(handle, "OneSwarm");
				RegUtil.RegCloseKey(handle);
				if (value != null) {
					COConfigurationManager.setParameter("autostart", true);
				} else {
					COConfigurationManager.setParameter("autostart", false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// create config listener
			COConfigurationManager.addAndFireParameterListener("autostart", new ParameterListener() {
				public void parameterChanged(String parameterName) {
					try {
						int handle = RegUtil.RegOpenKey(RegUtil.HKEY_CURRENT_USER, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run", RegUtil.KEY_ALL_ACCESS)[RegUtil.NATIVE_HANDLE];
						byte[] value = RegUtil.RegQueryValueEx(handle, "OneSwarm");
						boolean registryValue = value != null;
						if (registryValue != COConfigurationManager.getBooleanParameter("autostart")) {
							if (COConfigurationManager.getBooleanParameter("autostart")) {
								// install the registry key
								RegUtil.RegSetValueEx(handle, "OneSwarm", "\"" + SystemProperties.getApplicationPath() + "OneSwarm.exe\" --autostart");
							} else {
								// remove the registry key
								RegUtil.RegDeleteValue(handle, "OneSwarm");
							}
						}
						RegUtil.RegCloseKey(handle);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	private void installRemoteAccessPropertyListener() {
		COConfigurationManager.addParameterListener(OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY, new ParameterListener() {
			public void parameterChanged(String parameterName) {
				disableRemoteAccess();
				if (isRemoteAccessAllowed()) {
					try {
						enableRemoteAccess();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					disableRemoteAccess();
				}
			}
		});
	}

	private boolean isRemoteAccessAllowed() {
		return COConfigurationManager.getBooleanParameter(OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY);
	}

	public void shutdown() {
		logger.fine("Shutting down");
		try {
			server.stop();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		coreInterface.shutdown();
	}

	private static class ExtraSaltHashUserRealm extends HashUserRealm {
		private final boolean useSha1;

		public ExtraSaltHashUserRealm(boolean useSha1, String realm, String authFile) throws IOException {
			super(realm, authFile);
			this.useSha1 = useSha1;
		}

		@Override
		public Principal authenticate(String username, Object credentials, Request request) {
			try {

				if (useSha1) {
					String sha1SaltAndPassword = RemoteAccessConfig.getSaltedPassword((String) credentials);
					logger.finest("got authentication request: user=" + username + " salt_sha1=" + sha1SaltAndPassword);
					Principal principal = super.authenticate(username, sha1SaltAndPassword, request);
					return principal;
				} else {
					return super.authenticate(username, credentials, request);
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;

		}

		@Override
		public void setSingleSignOn(Request request, Response response, Principal principal, Credential credential) {
			Debug.out("set single sign-on called");
			super.setSingleSignOn(request, response, principal, credential);
		}

		@Override
		public Credential getSingleSignOn(Request request, Response response) {
			Debug.out("getSingleSignOn");
			return super.getSingleSignOn(request, response);
		}

	}

}
