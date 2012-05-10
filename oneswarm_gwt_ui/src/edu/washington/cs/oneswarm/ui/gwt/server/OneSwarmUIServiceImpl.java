package edu.washington.cs.oneswarm.ui.gwt.server;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.StringList;
import org.gudy.azureus2.core3.config.impl.ConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilEncodingException;
import org.gudy.azureus2.core3.stats.transfer.OverallStats;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrent.TOTorrentProgressListener;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentImpl;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.plugins.download.DownloadManager;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;
import org.gudy.azureus2.plugins.torrent.TorrentFile;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.db.DHTDB;
import com.aelitis.azureus.core.dht.db.DHTDBValue;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;
import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.dht.DHTPluginContact;
import com.aelitis.azureus.plugins.dht.DHTPluginOperationListener;
import com.aelitis.azureus.plugins.dht.DHTPluginValue;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;

import edu.uw.cse.netlab.testharness.CLI_Main;
import edu.washington.cs.oneswarm.f2f.FileCollection;
import edu.washington.cs.oneswarm.f2f.FileListFile;
import edu.washington.cs.oneswarm.f2f.Friend;
import edu.washington.cs.oneswarm.f2f.FriendInvitation;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.f2f.TextSearchResult;
import edu.washington.cs.oneswarm.f2f.chat.Chat;
import edu.washington.cs.oneswarm.f2f.chat.ChatDAO;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1DownloadManager;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager;
import edu.washington.cs.oneswarm.f2f.permissions.GroupBean;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;
import edu.washington.cs.oneswarm.f2f.servicesharing.ClientService;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;
import edu.washington.cs.oneswarm.f2f.servicesharing.SharedService;
import edu.washington.cs.oneswarm.f2f.share.ShareManagerTools;
import edu.washington.cs.oneswarm.ui.gwt.BackendErrorLog;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.CoreTools;
import edu.washington.cs.oneswarm.ui.gwt.RemoteAccessConfig;
import edu.washington.cs.oneswarm.ui.gwt.RemoteAccessForward;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmGWT;
import edu.washington.cs.oneswarm.ui.gwt.client.OneSwarmRPCClient;
import edu.washington.cs.oneswarm.ui.gwt.client.i18n.OSMessages;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.FileTypeFilter;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.Strings;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.SwarmsBrowser;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.CommunityServerAddPanel;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.friends.wizard.FriendsImportCommunityServer;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.MagicWatchType;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendErrorReport;
import edu.washington.cs.oneswarm.ui.gwt.rpc.BackendTask;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ClientServiceDTO;
import edu.washington.cs.oneswarm.ui.gwt.rpc.CommunityRecord;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileListLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileTree;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInfoLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendInvitationLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FriendList;
import edu.washington.cs.oneswarm.ui.gwt.rpc.LocaleLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.SecurityLevel;
import edu.washington.cs.oneswarm.ui.gwt.rpc.FileInfo;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmUIService;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PagedTorrentInfo;
import edu.washington.cs.oneswarm.ui.gwt.rpc.PermissionsGroup;
import edu.washington.cs.oneswarm.ui.gwt.rpc.ReportableException;
import edu.washington.cs.oneswarm.ui.gwt.rpc.SerialChatMessage;
import edu.washington.cs.oneswarm.ui.gwt.rpc.SharedServiceDTO;
import edu.washington.cs.oneswarm.ui.gwt.rpc.SpeedTestResult;
import edu.washington.cs.oneswarm.ui.gwt.rpc.StringTools;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TextSearchResultLite;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentInfo;
import edu.washington.cs.oneswarm.ui.gwt.rpc.TorrentList;
import edu.washington.cs.oneswarm.ui.gwt.rpc.UnknownUserException;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager.CancellationListener;
import edu.washington.cs.oneswarm.ui.gwt.server.StatelessSwarmFilter.SortMetric;
import edu.washington.cs.oneswarm.ui.gwt.server.community.CommunityServerManager;
import edu.washington.cs.oneswarm.ui.gwt.server.community.KeyPublishOp;
import edu.washington.cs.oneswarm.ui.gwt.server.community.PublishSwarmsThread;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegAsyncOperationManager;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegAsyncOperationManager.DataNotAvailableException;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegException;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegWrapper;
import edu.washington.cs.oneswarm.watchdir.MagicDecider;
import edu.washington.cs.oneswarm.watchdir.MagicDirectoryManager;
import edu.washington.cs.oneswarm.watchdir.UpdatingFileTree;
import edu.washington.cs.oneswarm.watchdir.UpdatingFileTreeListener;

public class OneSwarmUIServiceImpl extends RemoteServiceServlet implements OneSwarmUIService {
    private static Logger logger = Logger.getLogger(OneSwarmUIServiceImpl.class.getName());
    // private final OsgwtuiMain parent;
    private AddTorrentManager addTorrentManager;
    private DownloadManager downloadManager;
    private CoreInterface coreInterface;
    private final DataUsageOperation usagestats = new DataUsageOperation();
    // private DataUsageOperation usagestats = null;
    private StatelessSwarmFilter mSwarmFilter = null;
    private int mLastCount;
    private boolean stopped = false;
    private boolean firstRun = true;
    private boolean recentchanges = false;

    private final boolean LOG_REQUEST_TIMES = false;

    static class RpcProfiling implements Comparable<RpcProfiling> {
        long totalTime;
        long totalCalls;
        final String method;
        LinkedList<Long> last1000calls = new LinkedList<Long>();
        static final int HISTORY_LEN = 1000;

        private RpcProfiling(String method) {
            super();
            this.method = method;
        }

        public double getAvgCallTime() {
            return totalTime / (double) totalCalls;
        }

        public void addEntry(long callTime) {
            totalCalls++;
            totalTime += callTime;

            last1000calls.addFirst(callTime);
            if (last1000calls.size() > HISTORY_LEN) {
                last1000calls.removeLast();
            }
        }

        public long getMedianTime() {
            if (last1000calls.size() < 1) {
                return -1;
            } else {
                ArrayList<Long> toSort = new ArrayList<Long>(last1000calls);
                Collections.sort(toSort);
                return toSort.get(toSort.size() / 2);
            }
        }

        @Override
        public String toString() {
            return method + " calls=" + totalCalls + " callTime=" + totalTime + " avg="
                    + getAvgCallTime() + " median=" + getMedianTime();
        }

        @Override
        public int compareTo(RpcProfiling o) {
            if (o.totalTime < totalTime) {
                return -1;
            } else if (o.totalTime == totalTime) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    private final HashMap<String, RpcProfiling> rpcProfilingMap = new HashMap<String, RpcProfiling>();

    private void updateProfileInfo(String method, long time) {
        synchronized (rpcProfilingMap) {
            RpcProfiling p = rpcProfilingMap.get(method);
            if (p == null) {
                p = new RpcProfiling(method);
                rpcProfilingMap.put(method, p);
            }
            p.addEntry(time);
        }
    }

    @Override
    public String processCall(String payload) throws SerializationException {
        if (!LOG_REQUEST_TIMES) {
            return super.processCall(payload);
        } else {
            try {
                long startTime = System.currentTimeMillis();
                RPCRequest rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
                onAfterRequestDeserialized(rpcRequest);
                Method method = rpcRequest.getMethod();
                String response = RPC.invokeAndEncodeResponse(this, method,
                        rpcRequest.getParameters(), rpcRequest.getSerializationPolicy());
                updateProfileInfo(method.getName(), System.currentTimeMillis() - startTime);
                return response;
            } catch (IncompatibleRemoteServiceException ex) {
                log("An IncompatibleRemoteServiceException was thrown while processing this call.",
                        ex);
                return RPC.encodeResponseForFailure(null, ex);
            }
        }
    }

    public static void loadLogger() throws SecurityException, FileNotFoundException, IOException {
        loadLogger(false);
    }

    public static void loadLogger(boolean force) throws SecurityException, FileNotFoundException,
            IOException {
        File logConfig = new File("./logging.properties");
        if (logConfig.exists()) {
            System.err.println("Log file exists, attempting load...");
            final LogManager logManager = LogManager.getLogManager();
            synchronized (LogManager.class) {
                Enumeration<String> loggers = logManager.getLoggerNames();
                // check if we have any non null loggers
                boolean logConfLoaded = false;
                if (!force) {
                    while (loggers.hasMoreElements()) {
                        final String l = loggers.nextElement();
                        if (l.length() > 0 && Logger.getLogger(l).getLevel() != null) {
                            logConfLoaded = true;
                        }
                    }
                }
                if (!logConfLoaded) {
                    logManager.readConfiguration(new FileInputStream(logConfig));
                    System.err.println("read log configuration: " + logConfig.getCanonicalPath());
                    Enumeration<String> loggerNames = logManager.getLoggerNames();
                    while (loggerNames.hasMoreElements()) {
                        final String l = loggerNames.nextElement();
                        System.err.println("log: " + l + " " + Logger.getLogger(l).getLevel());
                    }
                }
            }
        } else {
            System.err.println("logging disabled (no log file: " + logConfig.getCanonicalPath()
                    + ")");
        }
    }

    /*
     * gwt needs an empty constructor when running in hosted mode
     */
    public OneSwarmUIServiceImpl() {
        this(false);
        // must mean that we are running hosted mode...
        this.hostedMode = true;
        System.err.println("running in hosted mode");

    }

    public OneSwarmUIServiceImpl(boolean remote) {
        this.remoteAccess = remote;
        COConfigurationManager.setParameter("File.Decoder.Default", "UTF-8");

        try {
            /*
             * check if the logging is initialized, if not load it
             */
            loadLogger();

        } catch (IOException e) {
            Debug.out("error loading logger", e);
        }
    }

    private final boolean remoteAccess;

    public OneSwarmUIServiceImpl(CoreInterface coreInterface, boolean remote) {
        this(remote);

        downloadManager = coreInterface.getDownloadManager();
        this.coreInterface = coreInterface;
        this.addTorrentManager = new AddTorrentManager(coreInterface);
        mSwarmFilter = coreInterface.getSwarmFilter();
    }

    /**
	 *
	 */
    private static final long serialVersionUID = -3609953294130895972L;

    Thread metaInfoPruner = null;
    private boolean hostedMode = false;

    /**
     * Modified to load the .rpc serialization policy file from the classloader
     * instead of from the servlet context
     */
    @Override
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request,
            String moduleBaseURL, String strongName) {
        // The request can tell you the path of the web app relative to the
        // container root.
        String contextPath = request.getContextPath();

        String modulePath = null;
        if (moduleBaseURL != null) {
            try {
                modulePath = new URL(moduleBaseURL).getPath();
            } catch (MalformedURLException ex) {
                // log the information, we will default
                this.log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            }
        }

        SerializationPolicy serializationPolicy = null;

        /*
         * Check that the module path must be in the same web app as the servlet
         * itself. If you need to implement a scheme different than this,
         * override
         * this method.
         */
        if (modulePath == null || !modulePath.startsWith(contextPath)) {
            String message = "ERROR: The module path requested, "
                    + modulePath
                    + ", is not in the same web application as this servlet, "
                    + contextPath
                    + ".  Your module may not be properly configured or your client and server code maybe out of date.";
            this.log(message);
        } else {
            // Strip off the context path from the module base URL. It should be
            // a
            // strict prefix.
            String contextRelativePath = modulePath.substring(contextPath.length());

            String serializationPolicyFilePath = SerializationPolicyLoader
                    .getSerializationPolicyFileName(contextRelativePath + strongName);

            // Open the RPC resource file and read its contents.
            // InputStream is = servlet.getServletContext().getResourceAsStream(
            // serializationPolicyFilePath);

            InputStream is = getClass().getResourceAsStream(serializationPolicyFilePath);
            try {
                if (is != null) {
                    try {
                        serializationPolicy = SerializationPolicyLoader.loadFromStream(is, null);
                    } catch (ParseException e) {
                        this.log("ERROR: Failed to parse the policy file '"
                                + serializationPolicyFilePath + "'", e);
                    } catch (IOException e) {
                        this.log("ERROR: Could not read the policy file '"
                                + serializationPolicyFilePath + "'", e);
                    }
                } else {
                    String message = "ERROR: The serialization policy file '"
                            + serializationPolicyFilePath
                            + "' was not found; did you forget to include it in this deployment?";
                    this.log(message);
                }
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore this error
                    }
                }
            }
        }

        return serializationPolicy;
    }

    @Override
    public Boolean startBackend() throws OneSwarmException {
        try {

            // DEBUG: uncomment this to induce a startup error to test the error
            // reporting code
            // if( System.currentTimeMillis() > 0 )
            // AzureusCoreImpl.getSingleton().getCryptoManager().hashCode(); //
            // null pointer

            // this starts the Azureus core, which will start the plugin
            // interface, which will create the core interface that we need. oy!
            if (AzureusCoreImpl.isCoreAvailable() == false) {
                logger.fine("starting backend...");
                new CLI_Main(new String[] { "-keep_last" });
                logger.fine("backend is started");
                coreInterface = new CoreInterface(AzureusCoreImpl.getSingleton().getPluginManager()
                        .getDefaultPluginInterface());
                mSwarmFilter = coreInterface.getSwarmFilter();
                downloadManager = coreInterface.getDownloadManager();
                this.addTorrentManager = new AddTorrentManager(coreInterface);

            } else {
                logger.fine("backend is already started");
            }

            /**
             * Make sure the magic directory scanner is running
             */
            MagicDirectoryManager.get();

            /**
             * 10 minutes after startup and day thereafter
             */
            if (metaInfoPruner == null) {
                metaInfoPruner = new Thread("Metainfo directory pruning") {
                    @Override
                    public void run() {
                        try {

                            Thread.sleep(10 * 60 * 1000);

                            while (true) {
                                edu.washington.cs.oneswarm.MetaInfoPruner.get().prune();
                                Thread.sleep(86400000);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                metaInfoPruner.setDaemon(true);
                metaInfoPruner.start();
            }

            /**
             * Print out versions of everything to make logging a bit easier
             */
            try {
                for (PluginInterface plug : AzureusCoreImpl.getSingleton().getPluginManager()
                        .getPlugins()) {
                    System.err.println(plug.getPluginName() + " " + plug.getPluginVersion());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("Exception during startBackend: " + e.toString());
            e.printStackTrace();
            throw new OneSwarmException(getStackTraceAsString(e));
        }

        return COConfigurationManager.getBooleanParameter("oneswarm.beta.updates");
    }

    @Override
    public String getVersion(String session) {
        try {
            if (this.passedSessionIDCheck(session) == false) {
                throw new Exception("bad cookie");
            }

            return Constants.AZUREUS_VERSION;

        } catch (Exception e) {
            // fail silently
        }
        return "Unknown";
    }

    @Override
    public boolean recentFriendChanges(String session) {
        boolean result = false;
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("failed SessionIdCheck");
            }
            if (recentchanges) {
                result = true;
                recentchanges = false;
            }
        } catch (Exception E) {
            E.printStackTrace();
        }
        return result;
    }

    @Override
    public void setRecentChanges(String session, boolean value) {
        if (this.passedSessionIDCheck(session) == false) {
            try {
                throw new Exception("bad cookie");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        recentchanges = value;
    }
    
    /**
     * Returns an array of FileItems, a compact format for transmitting information about files stored on the server.
     * @param path The directory path to display the contents of.
     * @return Array of FileItems's one for each file or directory in the given path.
     * @author Nick Martindell
     */
    @Override
    public FileInfo[] listFiles(String session, String path) {
    	try {
            if (this.passedSessionIDCheck(session) == false) {
                throw new Exception("bad cookie");
            }
	
			//Empty path returns root dirs, otherwise list dirs under path
			File[] directory;
			if (path.equalsIgnoreCase("")) {
				directory = File.listRoots();
			} else {
				directory = new File(path).listFiles();
				Arrays.sort(directory);
			}
			
			FileInfo[] files = new FileInfo[directory.length];
			for (int i = 0; i < files.length; i++) {
				String name = directory[i].getName();
				if (name.equalsIgnoreCase(""))
					name = "/";

				files[i] = new FileInfo(directory[i].getAbsolutePath(), name, directory[i].isDirectory(), directory[i].canRead());
			}
			return files;
    	} catch (Exception e) {
    		e.printStackTrace();
    		return null;
    	}
	}

    @Override
    public Boolean createSwarmFromLocalFileSystemPath(final String session, final String basePath,
            final ArrayList<String> paths, final boolean startSeeding, final String announce,
            final ArrayList<PermissionsGroup> inPermittedGroups) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
            if (remoteAccess) {
                throw new Exception("call not allowed over remote access");
            }

            if (paths.size() == 0) {
                return false;
            }

            final BackendTaskManager tasks = BackendTaskManager.get();

            Thread creationThread = (new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int current_index = 0; current_index < paths.size(); current_index++) {
                        System.out.println("hashing: current index: " + current_index);

                        int task_id = -1;

                        try {
                            //TorrentInfo info = null;
                            String path = paths.get(current_index);
                            File file = new File(path);

                            /**
                             * If this is a multi-directory creation, include
                             * swarm diretory tags
                             */
                            String baseParent = (new File(basePath)).getParent();
                            String tagPath = null;
                            if (COConfigurationManager.getBooleanParameter("oneswarm.directory.tags")) {
                                tagPath = MagicDirectoryManager.computeTags(baseParent, path);
                            }

                            final TOTorrentCreator currentCreator = TOTorrentFactory
                                    .createFromFileOrDirWithComputedPieceLength(file, new URL(
                                            announce), true);

                            task_id = tasks.createTask("Hashing " + (current_index + 1) + " of "
                                    + paths.size(), new BackendTaskManager.CancellationListener() {
                                @Override
                                public void cancelled(int inID) {
                                    System.out.println("cancelled hashing: " + inID);
                                    currentCreator.cancel();
                                }
                            });
                            tasks.getTask(task_id).setSummary(file.getName());

                            final int task_id_shadow = task_id;
                            currentCreator.addListener(new TOTorrentProgressListener() {
                                @Override
                                public void reportCurrentTask(String task_description) {
                                    System.out.println("creating: " + task_description);
                                    if (task_description.equals("Operation cancelled")) {
                                        System.out.println("cancelled");
                                        tasks.removeTask(task_id_shadow); // might
                                        // be
                                        // gone
                                        // already
                                        // ,
                                        // but
                                        // just
                                        // in
                                        // case
                                        // this
                                        // is
                                        // an
                                        // internally
                                        // generated
                                        // cancel
                                        // .
                                    }
                                }

                                @Override
                                public void reportProgress(int percent_complete) {
                                    if (tasks.getTask(task_id_shadow) != null) {
                                        tasks.getTask(task_id_shadow).setProgress(
                                                percent_complete + "%");
                                    }
                                }
                            });

                            TOTorrent created = currentCreator.create();
                            if (created == null) {
                                System.err.println("created == null, canceled?");
                                break;
                            }

                            System.out.println("created");

                            try {
                                LocaleTorrentUtil.setDefaultTorrentEncoding(created);
                            } catch (LocaleUtilEncodingException e1) {
                                e1.printStackTrace();
                            }

                            System.out.println("setdefaultencoding");

                            // done now
                            tasks.removeTask(task_id);

                            System.out.println("removed task_id: " + task_id);

                            String configSavePath = COConfigurationManager
                                    .getStringParameter("General_sDefaultTorrent_Directory");
                            File outTorrent = null;
                            if (configSavePath == null) {
                                outTorrent = new File(file.getParentFile().getAbsolutePath(), file
                                        .getName() + ".torrent");
                            } else {
                                outTorrent = new File(configSavePath, file.getName() + ".torrent");
                            }

                            System.out.println("saving to: " + outTorrent.getAbsolutePath());

                            created.serialiseToBEncodedFile(outTorrent);

                            System.out.println("saved..");

                            MagicDirectoryManager.generate_preview_for_torrent(created, file);

                            /**
                             * Add the swarm and then define permissions.
                             * There's a chance we'll get a request for
                             * permissions of this swarm before we set them
                             * (since we aren't locking), but since the default
                             * permissions of new objects are deny everything,
                             * such a request would be equivalent to requesting
                             * just before we added this (which is fine).
                             */
                            System.out.println("converting groups");
                            ArrayList<GroupBean> converted_groups = new ArrayList<GroupBean>(
                                    inPermittedGroups.size());
                            for (PermissionsGroup g : inPermittedGroups) {
                                GroupBean bean = PermissionsDAO.get().getGroup(g.getGroupID());
                                if (bean != null) {
                                    converted_groups.add(bean);
                                } else {
                                    System.err.println("tried to add group to new swarm: "
                                            + (new String(created.getName())) + " " + g.getName()
                                            + " that we weren't aware of internally!");
                                }
                            }
                            System.out.println("setgroupsforhash");
                            PermissionsDAO.get().setGroupsForHash(
                                    ByteFormatter.encodeString(created.getHash()),
                                    converted_groups, true);

                            AzureusCore core = AzureusCoreImpl.getSingleton();
                            // TODO: massive amount of error checking to add
                            // here.
                            final org.gudy.azureus2.core3.download.DownloadManager dm = core
                                    .getGlobalManager()
                                    .addDownloadManager(
                                            outTorrent.getAbsolutePath(),
                                            null,
                                            file.getAbsolutePath(),
                                            org.gudy.azureus2.core3.download.DownloadManager.STATE_WAITING,
                                            true, true, null);

                            if (tagPath != null) {
                                dm.getDownloadState().setListAttribute(
                                        FileCollection.ONESWARM_TAGS_ATTRIBUTE,
                                        new String[] { tagPath.toString() });
                            }

                            /**
                             * this is taken from the MagicDirectoryManager add
                             * torrent listener -- unlike this one, however, we
                             * don't want to stop as soon as seeding status is
                             * reached since this might be a public torrent
                             */
                            dm.addListener(new org.gudy.azureus2.core3.download.DownloadManagerListener() {
                                @Override
                                public void completionChanged(
                                        org.gudy.azureus2.core3.download.DownloadManager manager,
                                        boolean completed) {
                                }

                                @Override
                                public void downloadComplete(
                                        org.gudy.azureus2.core3.download.DownloadManager manager) {
                                }

                                @Override
                                public void filePriorityChanged(
                                        org.gudy.azureus2.core3.download.DownloadManager download,
                                        org.gudy.azureus2.core3.disk.DiskManagerFileInfo file) {
                                }

                                @Override
                                public void positionChanged(
                                        org.gudy.azureus2.core3.download.DownloadManager download,
                                        int oldPosition, int newPosition) {
                                }

                                @Override
                                public void stateChanged(
                                        org.gudy.azureus2.core3.download.DownloadManager manager,
                                        int state) {
                                    if (state == org.gudy.azureus2.core3.download.DownloadManager.STATE_SEEDING) {
                                        System.out.println("binding audio data for: "
                                                + dm.getDisplayName());
                                        MagicDirectoryManager.bind_audio_xml(dm);

                                        dm.removeListener(this);
                                    }
                                }
                            });

                            System.out.println("force start!");
                            dm.setForceStart(true);

                            System.out.println("addDownloadFromLocalTorrent done");

                        } catch (Exception e) {
                            e.printStackTrace();

                            if (tasks.getTask(task_id) != null) {
                                tasks.getTask(task_id).setGood(false);
                                tasks.getTask(task_id).setSummary(e.toString());
                            }

                            System.out.println("exception causes us to break!");

                            break;
                        }
                    }// for loop

                }
            }, "swarm creation"));
            creationThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static String getStackTraceAsString(Exception e) {
        StringWriter st = new StringWriter();
        e.printStackTrace(new PrintWriter(st));
        return st.toString();
    }

    @Override
    public ReportableException reportError(ReportableException inError) {
        try {
            Socket relay = new Socket(
                    InetAddress.getByName(OneSwarmConstants.ERROR_REPORTING_SERVER),
                    OneSwarmConstants.ERROR_REPORTING_PORT);
            DataOutputStream out = new DataOutputStream(relay.getOutputStream());

            //StringWriter backing = new StringWriter();

            byte[] bytes = inError.getText().getBytes();
            out.writeInt(bytes.length);
            out.write(bytes);

            out.flush();
            relay.close();
        } catch (Exception e) {
            return new ReportableException(getStackTraceAsString(e));
        }

        return null;
    }

    @Override
    public String ping(String session, String uiVersion) throws Exception {
        if (!passedSessionIDCheck(session)) {
            throw new Exception("Page reload required, session id missmatch.");
        }
        String currentVersion = getVersion(session);
        if (uiVersion != null && !currentVersion.equals(uiVersion)) {
            throw new Exception("OneSwarm updated");
        }
        return currentVersion;
    }

    @Override
    public TorrentList getTorrentsInfo(String session, int page) {

        TorrentList torrentList = null;

        try {
            // long startTime = System.currentTimeMillis();
            // check if the requestor knows the session id

            // long startTime = System.currentTimeMillis();
            if (!passedSessionIDCheck(session)) {
                return new TorrentList();
            }

            Download[] dowloadList = downloadManager.getDownloads(false);
            Download[] downloadsSorted = dowloadList.clone();
            Arrays.sort(downloadsSorted, getDownloadComparator());

            int firstTorrent = page * OneSwarmConstants.TORRENTS_PER_PAGE;
            int lastTorrent = Math.min((page + 1) * OneSwarmConstants.TORRENTS_PER_PAGE,
                    downloadsSorted.length);
            int torrentNum = lastTorrent - firstTorrent;
            if (torrentNum < 0) {
                return null;
            }
            TorrentInfo[] torrents = new TorrentInfo[torrentNum];
            for (int i = firstTorrent; i < lastTorrent; i++) {
                TorrentFile biggestFile = CoreTools.getBiggestVideoFile(downloadsSorted[i]);
                torrents[i] = TorrentInfoFactory.create(coreInterface.getF2FInterface(),
                        downloadsSorted[i], biggestFile == null ? "" : biggestFile.getName());
                // System.out.println("created a torrent info: " +
                // torrents[i].getName());
            }
            // System.out.println("created " + torrentNum + " torrentinfos in "
            // + (System.currentTimeMillis() - startTime) + " ms");

            torrentList = new TorrentList();
            torrentList.setTorrentInfos(torrents);
            torrentList.setTotalTorrentNum(downloadsSorted.length);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return torrentList;
    }

    @Override
    public TorrentList getTransferringInfo(String session) {
        TorrentList torrentList = null;

        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            Download[] downloads = downloadManager.getDownloads(false);
            List<TorrentInfo> outInfo = new ArrayList<TorrentInfo>();
            for (Download d : downloads) {
                /**
                 * including (possibly hundreds) of idle swarms causes the
                 * transferdetailstable to take forever to redraw in the
                 * browser, so we only include things that are actually
                 * _downloading_ or things that are seeding with >0 traffic
                 * 
                 * isdal: include ones that have peers or seeds connected as
                 * well
                 */
                boolean bytesExchanged = d.getStats().getUploadAverage() > 0
                        || d.getStats().getDownloadAverage() > 0;

                PeerManager peerManager = d.getPeerManager();
                boolean peersOrSeeds = false;
                if (peerManager != null) {
                    peersOrSeeds = peerManager.getPeers().length > 0;
                }
                if (d.getState() == Download.ST_DOWNLOADING || bytesExchanged || peersOrSeeds) {

                    TorrentFile biggestFile = CoreTools.getBiggestVideoFile(d);
                    outInfo.add(TorrentInfoFactory.create(coreInterface.getF2FInterface(), d,
                            biggestFile == null ? "" : biggestFile.getName()));
                }
            }

            torrentList = new TorrentList();
            torrentList.setTorrentInfos(outInfo.toArray(new TorrentInfo[0]));
            torrentList.setTotalTorrentNum(outInfo.size());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return torrentList;
    }

    @Override
    public HashMap<String, String> getSidebarStats(String session) {
        HashMap<String, String> map = null;

        if (usagestats == null) {
            return null;
        }

        if (firstRun) {
            usagestats.setCore(coreInterface);
            firstRun = false;
        }
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            map = new HashMap<String, String>();
            map.put(Strings.SIDEBAR_DL_RATE, "0");
            map.put(Strings.SIDEBAR_UL_RATE, "0");
            map.put(Strings.SIDEBAR_REMOTE, "0");
            map.put(Strings.SIDEBAR_REMOTE_IPS, "");
            map.put(Strings.SIDEBAR_COUNT, "0");
            map.put(Strings.SIDEBAR_TOTAL, "0");

            AzureusCore core = AzureusCoreImpl.getSingleton();

            if (AzureusCoreImpl.isCoreAvailable() == false) {
                return map;
            }

            org.gudy.azureus2.core3.global.GlobalManagerStats stats = core.getGlobalManager()
                    .getStats();

            map.put(Strings.SIDEBAR_DL_RATE, stats.getDataAndProtocolReceiveRate() + "");
            map.put(Strings.SIDEBAR_UL_RATE, stats.getDataAndProtocolSendRate() + "");
            map.put(Strings.SIDEBAR_COUNT, core.getGlobalManager().getDownloadManagers().size()
                    + "");
            map.put(Strings.SIDEBAR_REMOTE, coreInterface.getRemoteAccessRate());
            map.put(Strings.SIDEBAR_REMOTE_IPS, coreInterface.getRemoteAccessIps());
            /**
             * We can't do this every time the sidebar updates -- (once/second)
             * since it traverses every download!
             * 
             * Also, it tends to cause errors:
             * 
             * Caused by: java.lang.NullPointerException at
             * edu.washington.cs.oneswarm
             * .f2f.network.F2FDownloadManager.getState
             * (F2FDownloadManager.java:325) at
             * edu.washington.cs.oneswarm.f2f.network
             * .F2FDownloadManager.isSharedWithFriends
             * (F2FDownloadManager.java:294) at
             * edu.washington.cs.oneswarm.f2f.OSF2FPlugin
             * .isSharedWithFriends(OSF2FPlugin.java:122) at
             * edu.washington.cs.oneswarm
             * .ui.gwt.F2FInterface.isSharedWithFriends(F2FInterface.java:430)
             * at edu.washington.cs.oneswarm.ui.gwt.server.TorrentInfoFactory.
             * create(TorrentInfoFactory.java:60) at
             * edu.washington.cs.oneswarm.ui
             * .gwt.server.OneSwarmUIServiceImpl.getTorrentsInfo
             * (OneSwarmUIServiceImpl.java:657) at
             * edu.washington.cs.oneswarm.ui.
             * gwt.server.OneSwarmUIServiceImpl.getSidebarStats
             * (OneSwarmUIServiceImpl.java:754)
             */
            // TorrentList torrents =
            // getTorrentsInfo(OneSwarmRPCClient.getSessionID(), 0);
            // usagestats.getStats(torrents);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public HashMap<String, String> getLimits(String session) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
        } catch (Exception E) {
            E.printStackTrace();
        }
        return usagestats.getLimits();
    }

    @Override
    public HashMap<String, String> getCounts(String session) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
        } catch (Exception E) {
            E.printStackTrace();
        }
        return usagestats.getCounts();
    }

    @Override
    public HashMap<String, String> getDataStats(String session) {
        if (firstRun) {
            usagestats.setCore(coreInterface);
            firstRun = false;
        }
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
        } catch (Exception E) {
            E.printStackTrace();
        }
        TorrentList torrents = getTorrentsInfo(OneSwarmRPCClient.getSessionID(), 0);
        HashMap<String, String> dataMap = usagestats.getStats(torrents);
        if (dataMap.get("Stopped").equals("Stopped") && !stopped) {
            stopped = true;
            coreInterface.getF2FInterface().stopTransfers();
        }
        if (dataMap.get("Stopped").equals("Running") && stopped) {
            stopped = false;
            coreInterface.getF2FInterface().restartTransfers();
        }
        return dataMap;
    }

    @Override
    public String[] checkIfWarning(String session) {
        if (!passedSessionIDCheck(session)) {
            try {
                throw new Exception("bad cookie");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (usagestats != null) {
            return usagestats.checkWarning();
        }
        return null;
    }

    @Override
    public void resetLimit(String session, String limittype) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
        } catch (Exception E) {
            E.printStackTrace();
        }
        System.out.println("JUST BEFORE RESETLIMIT CALL");
        usagestats.resetLimit(limittype);
    }

    @Override
    public void setLimits(String session, String day, String week, String month, String year) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        usagestats.setLimits(day, week, month, year);
    }

    public boolean passedSessionIDCheck(String givenId) {
        if (hostedMode) {
            return true;
        }
        if (coreInterface.getSessionID().equals(givenId)) {
            return true;
        }
        System.out.println("Session id missmatch: " + givenId);

        return false;
    }

    private Comparator<Download> getDownloadComparator() {
        return new Comparator<Download>() {

            @Override
            public int compare(Download d1, Download d2) {
                if (d1.getCreationTime() > d2.getCreationTime()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        };
    }

    @Override
    public int downloadTorrent(String session, String path) {
        System.out.println("got torrent download request: '" + path + "'");
        if (!passedSessionIDCheck(session)) {
            return -1;
        }
        System.out.println("requesting download: '" + path + "'");

        return addTorrentManager.downloadTorrent(path);
    }

    @Override
    public void addDownloadFromLocalTorrentDefaultSaveLocation(String session,
            String inPathToTorrent, ArrayList<PermissionsGroup> inPermissions)
            throws OneSwarmException {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
            if (remoteAccess) {
                throw new Exception("call not allowed over remote access");
            }

            String defaultSave = COConfigurationManager.getDirectoryParameter("Default save path");

            File torrent = new File(inPathToTorrent);
            if (torrent.exists() == false) {
                throw new IOException("Torrent doesn't exist: " + inPathToTorrent);
            }

            File cand = new File(defaultSave);

            if (cand.exists() == false || cand.isDirectory() == false) {
                System.err
                        .println("default save directory doesn't exist (or is not a directory). using torrent parent dir instead...");
                cand = torrent.getParentFile();
            }

            addDownloadFromLocalTorrent(session, inPathToTorrent, cand.getAbsolutePath(), false,
                    inPermissions);
        } catch (Exception e) {
            throw new OneSwarmException(getStackTraceAsString(e));
        }
    }

    public void addDownloadFromLocalTorrent(String session, String path, String savePath,
            boolean skipCheck, List<PermissionsGroup> inPermissions) throws OneSwarmException {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
            if (remoteAccess) {
                throw new Exception("call not allowed over remote access");
            }

            System.out.println("attempting add for: " + path + " (save path: " + savePath + ")");

            File file = new File(path);
            if (!file.exists()) {
                throw new IOException("Tried to add file '" + path + "', but it doesn't exist");
            } else if (file.isDirectory()) {
                if (file.list().length == 0) {
                    throw new IOException("Tried to add files in '" + path
                            + "', but there are none");
                }
            }

            File savePathFile = file.getParentFile();
            if (savePath != null) {
                savePathFile = new File(savePath);
            }

            /**
             * We first need to grab the torrent hash so we can set the
             * permissions before adding the download manager (we do this so the
             * filelist regeneration code will have the proper permissions)
             */
            TOTorrent tor = TOTorrentFactory.deserialiseFromBEncodedFile(file);
            byte[] hashBytes = tor.getHash();
            if (inPermissions != null) {
                PermissionsDAO perms = PermissionsDAO.get();
                ArrayList<GroupBean> converted = new ArrayList<GroupBean>();
                for (PermissionsGroup g : inPermissions) {
                    GroupBean gb = GroupBean.createGroup(g.getName(), g.getKeys(), g.isUserGroup(),
                            g.getGroupID());
                    gb.setGroupName(g.getName());
                    gb.setMemberKeys(g.getKeys());
                    gb.setUserGroup(g.isUserGroup());
                    converted.add(gb);
                }
                perms.setGroupsForHash(ByteFormatter.encodeString(hashBytes), converted, true);
            } else {
                System.err.println("null permissions for added swarm: " + file.getName());
            }

            AzureusCore core = AzureusCoreImpl.getSingleton();
            // TODO: massive amount of error checking to add here.
            org.gudy.azureus2.core3.download.DownloadManager dm = null;

            if (skipCheck == false) {
                dm = core.getGlobalManager().addDownloadManager(path,
                        savePathFile.getAbsolutePath());
            } else {
                dm = core.getGlobalManager().addDownloadManager(path, null,
                        savePathFile.getAbsolutePath(),
                        org.gudy.azureus2.core3.download.DownloadManager.STATE_WAITING, true, true,
                        null);
            }
            if (coreInterface.getF2FInterface() != null) {

                /**
                 * Default: this helps get publicly shared things started
                 * quickly.
                 */

                coreInterface.getF2FInterface().setTorrentPrivacy(dm.getTorrent().getHash(),
                        PermissionsDAO.get().hasPublicPermission(hashBytes),
                        PermissionsDAO.get().hasAllFriendsPermission(hashBytes));
                /**
                 * We'll set the perms in the calling function
                 */
            }

            /**
             * sometimes this will add in queued state, which causes the
             * progress check to fail (so the preview will never be generated
             * and the file check won't happen). this avoids that
             */
            System.out.println("force start!");
            dm.setForceStart(true);

        } catch (OneSwarmException e) {
            throw e;
        } catch (Exception e) {
            throw new OneSwarmException(getStackTraceAsString(e));
        }
    }

    @Override
    public int downloadTorrent(String session, int friendConnection, int channelId,
            String torrentId, int lengthHint) {
        if (!passedSessionIDCheck(session)) {
            return -1;
        }
        System.out.println("requesting download: '" + torrentId + "'");
        return addTorrentManager
                .downloadTorrent(friendConnection, channelId, torrentId, lengthHint);
    }

    @Override
    public boolean startTorrent(String session, String[] torrentIDs) {
        if (!passedSessionIDCheck(session)) {
            return false;
        }

        try {
            for (String tid : torrentIDs) {
                coreInterface.startDownload(tid);
                System.out.println("started torrent: " + tid);
            }
            return true;
        } catch (DownloadException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean stopTorrent(String session, String[] torrentIDs) {
        if (!passedSessionIDCheck(session)) {
            return false;
        }
        try {
            for (String tid : torrentIDs) {
                coreInterface.stopDownload(tid);
                System.out.println("stopped torrent: " + tid);
            }
            return true;
        } catch (DownloadException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Boolean addTorrent(String session, int torrentDownloadID, FileListLite[] selectedFiles,
            ArrayList<PermissionsGroup> inPerms, String path, boolean noStream) {
        if (!passedSessionIDCheck(session)) {
            return false;
        }
        return addTorrentManager.addTorrent(torrentDownloadID, selectedFiles, inPerms, path,
                noStream);
    }

    @Override
    public FileListLite[] getTorrentFiles(String session, int torrentDownloadID) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("Session unknown, this should never happen!");
        }
        return addTorrentManager.getFiles(torrentDownloadID);
    }

    @Override
    public String getTorrentName(String session, int torrentDownloadID) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("Session unknown, this should never happen!");
        }
        return addTorrentManager.getTorrentName(torrentDownloadID);
    }

    @Override
    public Integer getTorrentDownloadProgress(String session, int torrentDownloadID) {
        if (!passedSessionIDCheck(session)) {
            return -1;
        }
        return addTorrentManager.getPercentageDone(torrentDownloadID);
    }

    @Override
    public Boolean torrentExists(String session, String torrentID) {
        if (!passedSessionIDCheck(session)) {
            return false;
        }
        Download download = coreInterface.getDownload(torrentID);
        return download != null;
    }

    @Override
    public ReportableException deleteCompletely(String session, String[] torrentID) {
        if (!passedSessionIDCheck(session)) {
            return new ReportableException("Bad session cookie");
        }

        for (String id : torrentID) {
            logger.fine("deleting torrent: " + id);
            mSwarmFilter.filterUntilDelete(getInfohashForTorrentID(id).getBytes());

            try {
                AzureusCore core = AzureusCoreImpl.getSingleton();
                org.gudy.azureus2.core3.download.DownloadManager dm = core.getGlobalManager()
                        .getDownloadManager(getInfohashForTorrentID(id));
                core.getGlobalManager().removeDownloadManager(dm, true, true);
            } catch (Exception e) {
                return new ReportableException(getStackTraceAsString(e));
            }
        }

        return null;
    }

    @Override
    public boolean deleteData(String session, String[] torrentID) {
        if (!passedSessionIDCheck(session)) {
            return false;
        }

        for (String id : torrentID) {

            mSwarmFilter.filterUntilDelete(getInfohashForTorrentID(id).getBytes());

            if (coreInterface.removeTorrent(id, false, true) == false) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ReportableException deleteFromShareKeepData(String session, String[] torrentID) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad session");
            }
            for (String id : torrentID) {
                mSwarmFilter.filterUntilDelete(getInfohashForTorrentID(id).getBytes());
                coreInterface.removeTorrent(id, true, false);
            }
            return null;
        } catch (Exception e) {
            return new ReportableException(getStackTraceAsString(e));
        }
    }

    @Override
    public FriendList getFriends(String session, int prevListId, boolean includeDisconnected,
            boolean includeBlocked) {
        if (!passedSessionIDCheck(session)) {
            return new FriendList(new FriendInfoLite[0]);
        }
        final FriendInfoLite[] friends = coreInterface.getF2FInterface().getFriends(
                includeDisconnected, includeBlocked);
        Arrays.sort(friends);
        FriendList list = new FriendList(friends);
        if (prevListId != 0 && list.getListId() == prevListId) {
            /*
             * nothing changed in the list, just return an empty list with the
             * same list id, except if the prev list id was 0, then we always
             * return the full list
             */
            return new FriendList(list.getListId());
        }
        return list;
    }

    @Override
    public void addFriend(String session, FriendInfoLite friendInfoLite, boolean testOnly)
            throws OneSwarmException {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("Session unknown, this should never happen!");
        }
        try {
            Friend f = new Friend(friendInfoLite.getSource(), friendInfoLite.getName(),
                    friendInfoLite.getPublicKey());
            f.setBlocked(friendInfoLite.isBlocked());
            f.setCanSeeFileList(friendInfoLite.isCanSeeFileList());
            // by default, limited=don't request file lists
            f.setRequestFileList(friendInfoLite.isCanSeeFileList());
            if (friendInfoLite.getLastConnectIp() != null
                    && friendInfoLite.getLastConnectIp() != "") {
                try {
                    f.setLastConnectIP(InetAddress.getByName(friendInfoLite.getLastConnectIp()));
                    f.setLastConnectPort(friendInfoLite.getLastConnectPort());
                } catch (UnknownHostException e) {
                }
            }
            Friend checkExists = coreInterface.getF2FInterface().getFriend(
                    friendInfoLite.getPublicKey());
            if (checkExists != null && !checkExists.isBlocked()) {
                throw new OneSwarmException("Friend already in friend list: '"
                        + checkExists.getNick() + "'");
            }
            if (coreInterface.getF2FInterface().getMyPublicKey()
                    .equals(new String(Base64.encode(f.getPublicKey())))) {
                throw new OneSwarmException("This is the local machine");
            }

            // check for duplicate names
            String name = f.getNick();
            for (FriendInfoLite fr : coreInterface.getF2FInterface().getFriends()) {
                if (name.equals(fr.getName())) {
                    if (testOnly) {
                        throw new OneSwarmException("Name already in use");
                    } else {
                        f.setNick(name + ".");
                        Debug.out("name already in use, adding '.'");
                    }
                }
            }

            f.setGroup(friendInfoLite.getGroup());

            if (!testOnly) {
                int numAdded = coreInterface.getF2FInterface().addFriend(f);
                recentchanges = true;
                if (numAdded == 0) {
                    throw new OneSwarmException("Friend already in friend list");
                }
            }
        } catch (InvalidKeyException e) {
            throw new OneSwarmException(e);
        }
    }

    @Override
    public FriendInfoLite[] scanXMLForFriends(String session, String xml) throws OneSwarmException {
        try {
            if (!this.passedSessionIDCheck(session)) {
                System.err.println("Bad cookie");
                return null;
            }

            Friend[] rawfriends = coreInterface.getF2FInterface().scanXMLForFriends(xml);
            if (rawfriends == null) {
                return null;
            }

            FriendInfoLite[] conv = new FriendInfoLite[rawfriends.length];

            int i = 0;
            for (Friend f : rawfriends) {
                conv[i++] = FriendInfoLiteFactory.createFriendInfo(f);
            }

            return conv;
        } catch (Exception e) {
            throw new OneSwarmException(getStackTraceAsString(e));
        }
    }

    @Override
    public void applySwarmPermissionChanges(String session, ArrayList<TorrentInfo> inSwarms) {
        try {
            if (passedSessionIDCheck(session) == false) {
                throw new Exception("bad cookie");
            }

            for (TorrentInfo t : inSwarms) {
                System.out.println("syncing perms: " + t.getName() + " "
                        + (t.getSharePublic() ? "pub" : "nopub") + " "
                        + (t.getShareWithFriends() ? "friends" : "nofriends"));

                Download d = coreInterface.getDownload(t.getTorrentID());
                coreInterface.getF2FInterface().setTorrentPrivacy(d.getTorrent().getHash(),
                        t.getSharePublic(), t.getShareWithFriends());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getMyPublicKey(String session) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("Session unknown, this should never happen!");
        }
        return coreInterface.getF2FInterface().getMyPublicKey();
    }

    @Override
    public FileListLite[] getFileList(String session, int connectionId, String filter,
            int startNum, int num, long maxCacheAge) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("Session unknown, this should never happen!");
        }
        return coreInterface.getF2FInterface()
                .getFileList(connectionId, filter, startNum, num, maxCacheAge)
                .toArray(new FileListLite[0]);
    }

    @Override
    public Integer sendSearch(String session, String searchString) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("Session unknown, this should never happen!");
        }
        return coreInterface.getF2FInterface().sendSearch(searchString);
    }

    public static HashWrapper getInfohashForTorrentID(String inID) {
        String hash32 = inID.replaceFirst(OneSwarmConstants.BITTORRENT_MAGNET_PREFIX, "");
        return new HashWrapper(Base32.decode(hash32));
    }

    // private void showInFinder(String path){
    // try {
    // coreInterface.getPluginInterface().getPlatformManager().showFile(path);
    // } catch (PlatformManagerException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }

    private void revealPath(String session, String path) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("bad cookie");
        }
        if (remoteAccess) {
            throw new RuntimeException("call not allowed over remote access");
        }

        // the built in azureus reveal will not move the finder window in
        // front of firefox, only use it for non osx
        String os = System.getProperty("os.name");
        if (!os.contains("Mac OS")) {
            try {
                coreInterface.getPluginInterface().getPlatformManager().showFile(path);
            } catch (PlatformManagerException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                File loc = new File(path);
                if (loc.isDirectory()) {
                    Runtime.getRuntime().exec(new String[] { "/usr/bin/open", path });
                } else {
                    // path = loc.getParentFile().getAbsolutePath();
                    String escapedPath = path.replace("\"", "\\\"");
                    String script = "tell application \"Finder\"\n" + "	activate\n"
                            + "	select (POSIX file \"" + escapedPath + "\")\n" + "end tell";
                    System.out.println(script);

                    System.out.println(Runtime.getRuntime()
                            .exec(new String[] { "/usr/bin/osascript", "-e", script }).waitFor()
                            + "");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ReportableException revealSwarmInFinder(String session, TorrentInfo[] inSwarms) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad session cookie");
            }

            if (remoteAccess) {
                throw new Exception("call not allowed over remote access");
            }
            for (TorrentInfo swarm : inSwarms) {
                AzureusCore core = AzureusCoreImpl.getSingleton();

                org.gudy.azureus2.core3.download.DownloadManager dm = core.getGlobalManager()
                        .getDownloadManager(getInfohashForTorrentID(swarm.getTorrentID()));

                // this only works if the torrent is running
                // File loc = dm.getDiskManager().getSaveLocation();

                File loc = dm.getSaveLocation();
                String path = null;
                path = loc.getAbsolutePath();
                System.out.println("open path: " + path);

                revealPath(session, path);
                // Runtime.getRuntime().exec(new String[]{"/usr/bin/open",
                // path});
            }
        } catch (Exception e) {
            return new ReportableException(getStackTraceAsString(e));
        }

        return null;
    }

    @Override
    public ReportableException openFileDefaultApp(String session, TorrentInfo[] inSwarms) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad session cookie");
            }

            if (remoteAccess) {
                throw new Exception("call not allowed over remote access");
            }

            for (TorrentInfo swarm : inSwarms) {
                Download download = coreInterface.getDownload(swarm.getTorrentID());
                TorrentFile biggest = CoreTools.getBiggestVideoFile(download);
                String path = null;
                if (biggest != null) {
                    DiskManagerFileInfo diskManagerFile = CoreTools.getDiskManagerFileInfo(biggest,
                            download);
                    path = diskManagerFile.getFile().getAbsolutePath();

                    System.out.println("open path: " + path);

                    Utils.launch(path);
                    // Runtime.getRuntime().exec(
                    // new String[] { "/usr/bin/open", path });

                } else {
                    // probably not a movie file, just reveal
                    revealSwarmInFinder(session, new TorrentInfo[] { swarm });
                }
            }
        } catch (Exception e) {
            return new ReportableException(getStackTraceAsString(e));
        }

        return null;
    }

    @Override
    public TextSearchResultLite[] getSearchResult(String session, int searchId) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("Session unknown, this should never happen!");
        }
        List<TextSearchResult> result = coreInterface.getF2FInterface().getSearchResult(searchId);
        ArrayList<TextSearchResultLite> r = new ArrayList<TextSearchResultLite>(result.size());
        for (TextSearchResult t : result) {

            String collectionName = t.getCollection().getName();
            String collectionId = t.getCollection().getUniqueID();
            int channelId = t.getFirstSeenChannelId();
            int connectionId = t.getFirstSeenConnectionId();
            long age = t.getFirstSeenTime();

            List<String> th = new ArrayList<String>();
            List<Long> delays = new ArrayList<Long>();
            List<Friend> friends = t.getThroughFriends();

            for (int i = 0; i < friends.size(); i++) {
                th.add(friends.get(i).getNick());
                delays.add(t.getDelays().get(i));
            }
            String[] throughFriends = th.toArray(new String[th.size()]);
            Long[] friendDelay = delays.toArray(new Long[delays.size()]);

            List<FileListFile> files = t.getCollection().getChildren();
            for (FileListFile fileListFile : files) {

                final TextSearchResultLite trl = new TextSearchResultLite(age, channelId,
                        collectionId, collectionName, connectionId, fileListFile.getFileName(),
                        fileListFile.getLength(), friendDelay, throughFriends, t.getCollection()
                                .getAddedTimeUTC());
                if (t.isInLibrary()) {

                    final org.gudy.azureus2.core3.download.DownloadManager dm = AzureusCoreImpl
                            .getSingleton().getGlobalManager()
                            .getDownloadManager(new HashWrapper(Base64.decode(collectionId)));
                    if (dm != null) {
                        TOTorrentFile biggestVideoFile = CoreTools.getBiggestFile(dm, true);
                        try {
                            trl.setInLibrary(TorrentInfoFactory.create(coreInterface
                                    .getF2FInterface(), dm, biggestVideoFile == null ? ""
                                    : biggestVideoFile.getRelativePath()));
                        } catch (AzureusCoreException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (TOTorrentException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                r.add(trl);
            }
        }
        Collections.sort(r, new Comparator<TextSearchResultLite>() {
            @Override
            public int compare(TextSearchResultLite o1, TextSearchResultLite o2) {
                int o1delay = 6000;
                if (o1.getFriendDelay().length > 0) {
                    o1delay = (int) o1.getFriendDelay()[0].longValue();
                }
                int o2delay = 6000;
                if (o1.getFriendDelay().length > 0) {
                    o2delay = (int) o2.getFriendDelay()[0].longValue();
                }

                if (o1delay < o2delay) {
                    return -1;
                } else if (o1delay == o2delay) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });

        int max = getIntegerParameterValue(session, "oneswarm.max.ui.search.results");
        TextSearchResultLite[] res = new TextSearchResultLite[Math.min(max, r.size())];
        for (int j = 0; j < res.length; j++) {
            res[j] = r.get(j);
        }
        return res;
    }

    @Override
    public FileTree getFiles(String session, String path) {

        // return FileTreeFactory.createFileTree(new File(path));
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("bad session cookie");
        }

        if (remoteAccess) {
            throw new RuntimeException("call not allowed over remote access");
        }

        File f = new File(path);
        if (f.isDirectory() == false || f.exists() == false) {
            System.err.println("selected dir doesn't exist! (or isn't a dir!)");
            return new FileTree();
        }

        UpdatingFileTree root = new UpdatingFileTree(f, new UpdatingFileTreeListener() {
            @Override
            public void broadcastChange(UpdatingFileTree path, boolean isDelete) {
            }
        });

        List<File> swarms = MagicDecider.decideTorrents(root, MagicWatchType.Magic,
                new ArrayList<String>(), null);
        Set<String> swarm_paths = new HashSet<String>();
        for (File f2 : swarms) {
            swarm_paths.add(f2.getAbsolutePath());
        }

        for (File checked : swarms) {
            System.out.println("checked: " + checked.getAbsolutePath());
        }

        return convert_helper(root, swarm_paths);
    }

    private FileTree convert_helper(UpdatingFileTree root, Set<String> checked) {
        FileTree curr = new FileTree();
        curr.setName(root.getThisFile().getName());
        try {
            curr.setFullpath(root.getThisFile().getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
            curr.setFullpath(root.getThisFile().getAbsolutePath());
        }
        FileTree[] kids = new FileTree[root.getChildren().size()];
        curr.setCheckedChild(false);
        for (int i = 0; i < kids.length; i++) {
            kids[i] = convert_helper(root.getChildren().get(i), checked);
            if (kids[i].getMagicCheck()) {
                curr.setCheckedChild(true);
            }
        }
        curr.setChildren(kids);

        if (checked.contains(root.getThisFile().getAbsolutePath())) {
            curr.setMagicCheck(true);
        } else {
            curr.setMagicCheck(false);
        }

        curr.sortRecursive();
        return curr;
    }

    @Override
    public ArrayList<HashMap<String, String>> getFriendTransferStats(String session) {
        if (!passedSessionIDCheck(session)) {
            System.err.println("failed session check");
            return null;
        }

        ArrayList<HashMap<String, String>> out = new ArrayList<HashMap<String, String>>();
        for (HashMap<String, String> e : (coreInterface.getF2FInterface().getTransferStats())) {
            out.add(e);
        }

        return out;
    }

    @Override
    public void setFriendsSettings(String session, FriendInfoLite[] updates) {
        if (!passedSessionIDCheck(session)) {
            System.err.println("failed session check");
            return;
        }

        for (FriendInfoLite updated : updates) {
            // check for duplicate names
            String name = updated.getName();
            for (FriendInfoLite fr : coreInterface.getF2FInterface().getFriends()) {
                if (!fr.getPublicKey().equals(updated.getPublicKey())) {
                    if (name.equals(fr.getName())) {
                        updated.setName(name + ".");
                        Debug.out("name already in use, adding '.'");
                    }
                }
            }
            coreInterface.getF2FInterface().setFriendSettings(updated.getPublicKey(),
                    updated.getName(), updated.isBlocked(), updated.isCanSeeFileList(),
                    updated.isAllowChat(), updated.isRequestFileList(), updated.getGroup());
        }
    }

    @Override
    public FriendList getPendingCommunityFriendImports(String session) throws OneSwarmException {
        if (!passedSessionIDCheck(session)) {
            System.err.println("failed session check on getPendingCommunityFriendImports()");
            throw new OneSwarmException("failed session check");
        }

        return CommunityServerManager.get().munch();
    }

    @Override
    public FriendInfoLite[] getNewUsersFromXMPP(String session, String xmppNetworkName,
            String username, char[] password, String machineName) throws OneSwarmException {

        if (!passedSessionIDCheck(session)) {
            System.err.println("failed session check");
            throw new OneSwarmException("failed session check");
        }
        // update the computer name
        COConfigurationManager.setParameter("Computer Name", machineName);

        try {
            return coreInterface.getF2FInterface().getNewUsersFromXMPP(xmppNetworkName, username,
                    password, machineName);
        } catch (Throwable e) {
            Debug.out("Got error when importing xmpp friends", e);
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            throw new OneSwarmException(cause.getMessage());
        }

    }

    @Override
    public int pollCommunityServer(String session, CommunityRecord record) throws OneSwarmException {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            KeyPublishOp pollThread = new KeyPublishOp(record, true);
            // we don't prune explicit requests -- this will be added with the
            // threshold if
            // successful
            int taskID = BackendTaskManager.get().createTask("Community server refresh...",
                    pollThread);
            BackendTaskManager.get().getTask(taskID).setSummary("Contacting server...");
            pollThread.setTaskID(taskID);
            pollThread.start();

            return taskID;

        } catch (Throwable e) {
            Debug.out("Got error when importing community friends", e);
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            throw new OneSwarmException(cause.getMessage());
        }
    }

    @Override
    public HashMap<String, Integer> getTorrentsState(String session) {

        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            HashMap<String, Integer> outStates = new HashMap<String, Integer>();

            for (Download d : downloadManager.getDownloads()) {
                String id = OneSwarmConstants.BITTORRENT_MAGNET_PREFIX
                        + new String(Base32.encode(d.getTorrent().getHash()));
                outStates.put(id, d.getState());
            }

            return outStates;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getComputerName(String session) {
        if (!passedSessionIDCheck(session)) {
            System.err.println("failed session check");
            return null;
        }

        try {
            String name = COConfigurationManager.getStringParameter("Computer Name", null);
            if (name == null) {
                InetAddress addr = InetAddress.getLocalHost();

                // Get hostname
                name = addr.getHostName();
                if (name != null && name.split("\\.").length > 0) {
                    name = name.split("\\.")[0];
                }
                COConfigurationManager.setParameter("Computer Name", name);
            }
            return name;
        } catch (UnknownHostException e) {
        }

        return "";
    }

    @Override
    public void setComputerName(String session, String computerName) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
            // update the computer name
            if (computerName.length() > 0 && computerName.length() < 64) {
                COConfigurationManager.setParameter("Computer Name", computerName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public TorrentInfo[] pagedTorrentStateRefresh(String session, ArrayList<String> whichOnes) {
        try {
            if (!passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            TorrentInfo[] outInfo = new TorrentInfo[whichOnes.size()];
            for (int i = 0; i < whichOnes.size(); i++) {
                String s = whichOnes.get(i);

                final org.gudy.azureus2.core3.download.DownloadManager dm = AzureusCoreImpl
                        .getSingleton().getGlobalManager()
                        .getDownloadManager(new HashWrapper(getInfohashForTorrentID(s).getHash()));

                if (dm != null) {
                    TOTorrentFile biggestVideoFile = CoreTools.getBiggestFile(dm, true);
                    outInfo[i] = TorrentInfoFactory.create(coreInterface.getF2FInterface(), dm,
                            biggestVideoFile == null ? "" : biggestVideoFile.getRelativePath());
                }
            }

            return outInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Integer getIntegerParameterValue(String session, String inParamName) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
            return null;
        } else {
            return COConfigurationManager.getIntParameter(inParamName);
        }
    }

    @Override
    public void setIntegerParameterValue(String session, String inParamName, Integer inValue) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
        } else {
            COConfigurationManager.setParameter(inParamName, inValue);
            ConfigurationManager.getInstance().setDirty();
        }
    }

    @Override
    public boolean getStopped(String session) {
        if (!passedSessionIDCheck(session)) {
            try {
                throw new Exception("bad cookie");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (usagestats != null) {
            return usagestats.getStopped();
        }
        return false;
    }

    @Override
    public Boolean getBooleanParameterValue(String session, String inParamName) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
            return null;
        } else {
            return COConfigurationManager.getBooleanParameter(inParamName);
        }
    }

    @Override
    public void setBooleanParameterValue(String session, String inParamName, Boolean inValue) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
        } else {
            COConfigurationManager.setParameter(inParamName, inValue);
            ConfigurationManager.getInstance().setDirty();
        }
    }

    @Override
    public String getStringParameterValue(String session, String inParamName) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
            return null;
        } else {
            return COConfigurationManager.getStringParameter(inParamName);
        }
    }

    @Override
    public void setStringParameterValue(String session, String inParamName, String inValue) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
        } else {
            COConfigurationManager.setParameter(inParamName, inValue);
            ConfigurationManager.getInstance().setDirty();
        }
    }

    @Override
    public ArrayList<String> getStringListParameterValue(String session, String inParamName) {
        StringList toPut = COConfigurationManager.getStringListParameter(inParamName);
        ArrayList<String> converted = new ArrayList<String>();
        for (int i = 0; i < (toPut).size(); i++) {
            converted.add((toPut).get(i));
        }
        return converted;
    }

    @Override
    public void setStringListParameterValue(String session, String inParamName,
            ArrayList<String> toPut) {
        if (!passedSessionIDCheck(session)) {
            System.err.println("bad session");
            return;
        }

        COConfigurationManager.setParameter(inParamName, toPut);
        ConfigurationManager.getInstance().setDirty();
    }

    @Override
    public int getDownloadManagersCount(String session) {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new IOException("bad cookie");
            }

            if (AzureusCoreImpl.isCoreAvailable() == false) {
                return 0;
            }

            if (AzureusCoreImpl.getSingleton().isStarted() == false) {
                return 0;
            }

            // return AzureusCoreImpl.getSingleton().getGlobalManager().
            // getDownloadManagers().size();
            if (mSwarmFilter.shouldClientSideUIRefresh()) {
                mSwarmFilter.willUpdate();
                return -1;
            } else {
                return mLastCount;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public PagedTorrentInfo getPagedAndFilteredSwarms(int inPage, int swarmsPerPage, String filter,
            int sort, String type, boolean includeF2F, int selectedFriendID, String inTagPath) {

        FileTypeFilter typedType = FileTypeFilter.getFromName(type);

        SortMetric typedMetric = null;
        if (sort == SwarmsBrowser.SORT_BY_NAME_ID) {
            typedMetric = SortMetric.Name;
        } else if (sort == SwarmsBrowser.SORT_BY_DATE_ID) {
            typedMetric = SortMetric.Date;
        } else if (sort == SwarmsBrowser.SORT_BY_SIZE_ID) {
            typedMetric = SortMetric.Size;
        }

        String[] filterKeywords = filter.split("\\s+");

        StatelessSwarmFilter.FilteredSwarmInfo filteredInfo = mSwarmFilter.filterSwarms(
                filterKeywords, typedMetric, typedType, includeF2F, selectedFriendID, inTagPath);

        List<TorrentInfo> out = new ArrayList<TorrentInfo>();
        for (int i = inPage * swarmsPerPage; out.size() < swarmsPerPage
                && i < filteredInfo.outSwarms.size(); i++) {
            try {
                TorrentFile biggestFile = null;
                TorrentInfo info = null;
                /**
                 * adapter implies F2F file collection (these aren't real
                 * download managers)
                 */
                if (filteredInfo.outSwarms.get(i) instanceof DownloadManagerAdapter == false) {
                    Download d = downloadManager.getDownload(filteredInfo.outSwarms.get(i)
                            .getTorrent().getHash());
                    if (d == null) {
                        continue;
                    }
                    biggestFile = CoreTools.getBiggestVideoFile(d);
                    info = TorrentInfoFactory.create(coreInterface.getF2FInterface(), d,
                            biggestFile == null ? "" : biggestFile.getName());

                    /**
                     * A special case is when we're viewing only a single
                     * friend's files and we are adding one of our download
                     * managers. We want to keep browsing of the friend's files
                     * consistent if we're sorting by date-added. So, if we have
                     * it, use the friend's date added time that we saved when
                     * generated the manager list
                     */
                    Long friendAdded = (Long) filteredInfo.outSwarms.get(i).getData(
                            "friend-added-time");
                    if (friendAdded != null) {
                        info.setAddedDate(friendAdded.longValue());
                        // Need to clear this since it's a signal to the sorting
                        // routine that we're building a friend-only list of
                        // swarms
                        filteredInfo.outSwarms.get(i).setData("friend-added-time", null);
                    }

                } else {
                    DownloadManagerAdapter da = (DownloadManagerAdapter) filteredInfo.outSwarms
                            .get(i);
                    String largestFileName = "";
                    if (da.getCollection() != null) {
                        List<FileListFile> kids = da.getCollection().getLargestFile().getChildren();
                        if (kids.size() == 1) {
                            largestFileName = kids.get(0).getFileName();
                        }
                    }
                    info = TorrentInfoFactory
                            .create(coreInterface.getF2FInterface(),
                                    (DownloadManagerAdapter) filteredInfo.outSwarms.get(i),
                                    largestFileName);
                }
                out.add(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long total_bytes = 0;
        for (org.gudy.azureus2.core3.download.DownloadManager dm : filteredInfo.outSwarms) {
            total_bytes += dm.getSize();
        }

        PagedTorrentInfo outInfo = new PagedTorrentInfo();
        outInfo.swarms = out.toArray(new TorrentInfo[0]);
        outInfo.total_swarms = filteredInfo.outSwarms.size();
        outInfo.filtered_count = filteredInfo.total_swarms_in_type - outInfo.total_swarms;
        outInfo.by_type_count = filteredInfo.total_swarms_in_type;
        outInfo.total_size = StringTools.formatRate(total_bytes);

        outInfo.tags = filteredInfo.tags;
        outInfo.truncated_tags = filteredInfo.truncated_tags;

        // mLastCount = filteredInfo.total_swarms_in_type;
        mLastCount = outInfo.total_swarms;

        return outInfo;
    }

    // public PagedTorrentInfo getPagedAndFilteredSwarms(int inPage, int
    // swarmsPerPage, String filter, String sort, String type, boolean
    // includeF2F, int selectedFriendID) {
    // System.out.println("selectedFriendID: " + selectedFriendID + " min: " +
    // Integer.MIN_VALUE);
    //
    // org.gudy.azureus2.core3.download.DownloadManager[] pruned =
    // mSwarmFilter.getManagers();
    //
    // // // first, include friends files?
    // pruned = mSwarmFilter.filterByIncludeFriends(pruned, includeF2F,
    // selectedFriendID);
    //
    // FileTypeFilter typedType = null;
    // if (type.equals("all"))
    // typedType = FileTypeFilter.All;
    // else if (type.equals("video"))
    // typedType = FileTypeFilter.Videos;
    // else if (type.equals("other"))
    // typedType = FileTypeFilter.Other;
    // else if (type.equals("audio"))
    // typedType = FileTypeFilter.Audio;
    // pruned = StatefulSwarmFilter.filterByType(pruned, typedType, includeF2F);
    //
    // int byTypeCount = pruned.length;
    // System.out.println("by type count: " + byTypeCount);
    //
    // // next, by keyword
    // pruned = StatefulSwarmFilter.filterByKeyword(pruned, filter, includeF2F);
    //
    // SortMetric typedMetric = null;
    // if (sort.equals(SwarmsBrowser.SORT_BY_NAME))
    // typedMetric = SortMetric.Name;
    // else if (sort.equals(SwarmsBrowser.SORT_BY_DATE))
    // typedMetric = SortMetric.Date;
    // else if (sort.equals(SwarmsBrowser.SORT_BY_SIZE))
    // typedMetric = SortMetric.Size;
    // pruned = StatefulSwarmFilter.getSortedByMetric(pruned, typedMetric,
    // includeF2F);
    //
    // List<TorrentInfo> out = new ArrayList<TorrentInfo>();
    // for (int i = inPage * swarmsPerPage; out.size() < swarmsPerPage && i <
    // pruned.length; i++) {
    // try {
    // TorrentFile biggestFile = null;
    // TorrentInfo info = null;
    // if (pruned[i] instanceof DownloadManagerAdapter == false) // adapter
    // // implies
    // // F2F
    // // FileCollection
    // // ,
    // // not
    // // a
    // // real
    // // download
    // // manager
    // {
    // Download d =
    // downloadManager.getDownload(pruned[i].getTorrent().getHash());
    // if(d==null){
    // continue;
    // }
    // biggestFile = CoreTools.getBiggestVideoFile(d);
    // info = TorrentInfoFactory.create(coreInterface.getF2FInterface(), d,
    // biggestFile == null ? "" : biggestFile.getName());
    // } else {
    // info = TorrentInfoFactory.create(coreInterface.getF2FInterface(),
    // (DownloadManagerAdapter) pruned[i]);
    // }
    // out.add(info);
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    //
    // PagedTorrentInfo outInfo = new PagedTorrentInfo();
    // outInfo.swarms = out.toArray(new TorrentInfo[0]);
    // outInfo.total_swarms = pruned.length;
    // outInfo.filtered_count = byTypeCount - pruned.length;
    // outInfo.by_type_count = byTypeCount;
    //
    // System.out.println("swarms len: " + outInfo.swarms.length +
    // " pruned len: " + pruned.length);
    //
    // System.out.println("setting lastRecomputeCount: " +
    // outInfo.total_swarms);
    // mSwarmFilter.setLastRecomputeCount(outInfo.total_swarms);
    //
    // return outInfo;
    // }

    @Override
    public FileListLite[] getFilesForDownloadingTorrentHash(String session, String inOneSwarmHash) {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("bad session");
            }

            AzureusCore core = AzureusCoreImpl.getSingleton();

            org.gudy.azureus2.core3.download.DownloadManager dm = core
                    .getGlobalManager()
                    .getDownloadManager(
                            new HashWrapper(OneSwarmHashUtils.bytesFromOneSwarmHash(inOneSwarmHash)));
            if (dm != null) {
                org.gudy.azureus2.core3.disk.DiskManagerFileInfo[] files = dm
                        .getDiskManagerFileInfo();
                String[] sha1List = dm.getDownloadState().getListAttribute(TOTorrentImpl.OS_SHA1);
                String[] ed2kList = dm.getDownloadState().getListAttribute(TOTorrentImpl.OS_ED2K);

                FileListLite[] lites = new FileListLite[files.length];
                TOTorrent torrent = dm.getTorrent();
                for (int i = 0; i < files.length; i++) {
                    org.gudy.azureus2.core3.disk.DiskManagerFileInfo f = files[i];
                    lites[i] = new FileListLite(new String(Base64.encode(torrent.getHash())),
                            dm.getDisplayName(), f.getFile(false).getName(), f.getLength(),
                            files.length, (new Date()).getTime(), 1, files[i].isSkipped(),
                            files[i].getDownloaded() == files[i].getLength());
                    if (sha1List != null && sha1List.length > i) {
                        lites[i].setSha1Hash(sha1List[i]);
                    }
                    if (ed2kList != null && ed2kList.length > i) {
                        lites[i].setEd2kHash(ed2kList[i]);
                    }
                }

                return lites;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public ReportableException updateSkippedFiles(String session, FileListLite[] lites) {
        System.out.println("updateSkippedFiles()");

        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("bad session");
            }

            if (lites.length == 0) {
                throw new Exception("need to update something, length 0");
            }

            AzureusCore core = AzureusCoreImpl.getSingleton();

            String oneswarmHash = lites[0].getCollectionId();

            org.gudy.azureus2.core3.download.DownloadManager dm = core.getGlobalManager()
                    .getDownloadManager(new HashWrapper(Base64.decode(oneswarmHash)));
            if (dm != null) {
                if (dm.getDiskManagerFileInfo().length != lites.length) {
                    throw new Exception(
                            "number of files in collection doesn't match number of specified filelistlite objects");
                }

                System.out.println("found DM, updating skipped");

                org.gudy.azureus2.core3.disk.DiskManagerFileInfo[] files = dm
                        .getDiskManagerFileInfo();
                for (int i = 0; i < files.length; i++) {
                    org.gudy.azureus2.core3.disk.DiskManagerFileInfo f = files[i];
                    // ignore skipped files, perhaps it just completed as they
                    // said to skip
                    if (f.getLength() == f.getDownloaded()) {
                        continue;
                    }

                    f.setSkipped(lites[i].isSkipped());
                    System.out.println("skipping " + f.getFile(false).getName() + " "
                            + Boolean.toString(lites[i].isSkipped()));
                }

                coreInterface.startDownload(OneSwarmHashUtils.createOneSwarmHash(dm.getTorrent()
                        .getHash()));
            } else {
                throw new Exception("attempting to update skip when download manager not found: "
                        + oneswarmHash);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ReportableException(e.toString());
        }

        return null;
    }

    @Override
    public ArrayList<PermissionsGroup> getAllGroups(String session) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
            return null;
        }

        ArrayList<PermissionsGroup> out = new ArrayList<PermissionsGroup>();

        PermissionsDAO perms = PermissionsDAO.get();
        for (GroupBean gb : perms.getAllGroups()) {
            if (gb == null) {
                System.err.println("skipping null group in getAllGroups() RPC");
                continue;
            }

            if (gb.getMemberKeys() != null) {
                out.add(new PermissionsGroup(gb.getGroupName(), gb.getMemberKeys().toArray(
                        new String[0]), gb.isUserGroup(), gb.getGroupID()));
            } else {
                System.err.println("null member keys for group: " + gb.getGroupName());
                out.add(new PermissionsGroup(gb.getGroupName(), new String[] {}, gb.isUserGroup(),
                        gb.getGroupID()));
            }
        }

        return out;
    }

    @Override
    public FriendInfoLite[] getLanOneSwarmUsers(String session) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
            return null;
        }

        return coreInterface.getF2FInterface().getLanOneSwarmUsers();
    }

    @Override
    public ArrayList<FriendInfoLite> getFriendsForGroup(String session, PermissionsGroup inGroup) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
            return null;
        }

        ArrayList<FriendInfoLite> out = new ArrayList<FriendInfoLite>();

        GroupBean group = PermissionsDAO.get().getGroup(inGroup.getGroupID());
        if (group == null) {
            return null;
        }

        for (String pub : group.getMemberKeys()) {
            Friend friend = coreInterface.getF2FInterface().getFriend(pub);

            // filter out friends that can't see anything if this is not their
            // particular user group.
            if (friend.isCanSeeFileList() && group.isUserGroup() == false) {
                out.add(FriendInfoLiteFactory.createFriendInfo(friend));
            }
        }

        return out;
    }

    @Override
    public PermissionsGroup updateGroupMembership(String session, PermissionsGroup inGroup,
            ArrayList<FriendInfoLite> inMembers) throws OneSwarmException {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new IOException("bad session: " + session);
            }

            List<String> inNewKeys = new ArrayList<String>(inMembers.size());
            for (FriendInfoLite f : inMembers) {
                inNewKeys.add(f.getPublicKey());
            }

            /**
             * If it doesn't have a group ID, need to create new group.
             * Otherwise, update existing.
             */
            if (inGroup.getGroupID() == 0) {
                inGroup.setGroupID(PermissionsDAO.get().addGroup(inGroup.getName(), inNewKeys,
                        false));
            } else {
                PermissionsDAO.get().updateGroupKeys(inGroup.getGroupID(), inNewKeys);
            }
        } catch (Exception e) {
            throw new OneSwarmException(getStackTraceAsString(e));
        }
        return inGroup;
    }

    @Override
    public ReportableException removeGroup(String session, Long inGroupID) {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new IOException("bad session");
            }

            PermissionsDAO.get().removeGroupID(inGroupID);

            return null;
        } catch (Exception e) {
            return new ReportableException(getStackTraceAsString(e));
        }
    }

    @Override
    public ArrayList<PermissionsGroup> getGroupsForSwarm(String session, TorrentInfo inSwarm) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: " + session);
            return null;
        }

        ArrayList<PermissionsGroup> out = new ArrayList<PermissionsGroup>();
        ArrayList<GroupBean> dao_groups = PermissionsDAO.get().getGroupsForHash(
                ByteFormatter.encodeString(OneSwarmHashUtils.bytesFromOneSwarmHash(inSwarm
                        .getTorrentID())));

        logger.fine("get groups for: " + inSwarm.getName() + " got " + dao_groups.size()
                + " from DAO");

        for (GroupBean gb : dao_groups) {
            PermissionsGroup g = new PermissionsGroup(gb.getGroupName(), gb.getMemberKeys()
                    .toArray(new String[0]), gb.isUserGroup(), gb.getGroupID());
            out.add(g);
        }

        return out;
    }

    @Override
    public ReportableException setGroupsForSwarm(String session, TorrentInfo inSwarm,
            ArrayList<PermissionsGroup> inGroups) {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("bad session: " + session);
            }

            PermissionsDAO perms = PermissionsDAO.get();
            ArrayList<GroupBean> converted = new ArrayList<GroupBean>();
            for (PermissionsGroup g : inGroups) {
                GroupBean gb = GroupBean.createGroup(g.getName(), g.getKeys(), g.isUserGroup(),
                        g.getGroupID());
                converted.add(gb);
            }
            perms.setGroupsForHash(ByteFormatter.encodeString(OneSwarmHashUtils
                    .bytesFromOneSwarmHash(inSwarm.getTorrentID())), converted, false);

            return null;
        } catch (Exception e) {
            return new ReportableException(getStackTraceAsString(e));
        }
    }

    @Override
    public void connectToFriends(String session, FriendInfoLite[] friendLites) {
        for (FriendInfoLite friendLite : friendLites) {
            coreInterface.getF2FInterface().connectToFriend(friendLite.getPublicKey());
        }
    }

    @Override
    public FriendInfoLite getUpdatedFriendInfo(String session, FriendInfoLite friendLite) {
        Friend f = coreInterface.getF2FInterface().getFriend(friendLite.getPublicKey());
        if (f == null) {
            String ownKey = coreInterface.getF2FInterface().getMyPublicKey();
            if (ownKey.equals(friendLite.getPublicKey())) {
                // this is ourself...
                FriendInfoLite me = new FriendInfoLite();
                me.setName("Me (" + getComputerName(session) + ")");
                return me;
            }

            return null;
        }

        FriendInfoLite newInfo = FriendInfoLiteFactory.createFriendInfo(f);
        return newInfo;
    }

    @Override
    public BackendTask[] getBackendTasks(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("didn't pass session id check getbackendtasks: " + session);
            return null;
        }
        return BackendTaskManager.get().getTasks();
    }

    @Override
    public BackendTask getBackendTask(String session, int inID) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("didn't pass session id check getbackendtasks: " + session);
            return null;
        }
        return BackendTaskManager.get().getTask(inID);
    }

    @Override
    public void cancelBackendTask(String session, int inID) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("didn't pass session id check getbackendtasks: " + session);
            return;
        }

        BackendTaskManager.get().cancelTask(inID);
    }

    StringBuffer async_debug = new StringBuffer();

    @Override
    public String debug(String session, String which) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("bad session: " + session);
            return null;
        }

        final DHTPluginOperationListener dht_op_listener = new DHTPluginOperationListener() {
            @Override
            public void complete(byte[] key, boolean timeout_occurred) {
                synchronized (async_debug) {
                    async_debug.append((new Date()) + ": " + Base64.encode(key)
                            + " completed, timeout: " + Boolean.toString(timeout_occurred) + "\n");
                }
            }

            @Override
            public void diversified() {
                synchronized (async_debug) {
                    async_debug.append((new Date()) + ": diversified\n");
                }
            }

            @Override
            public void valueRead(DHTPluginContact originator, DHTPluginValue value) {
                synchronized (async_debug) {
                    async_debug.append((new Date()) + ": valueRead: "
                            + Base64.encode(value.getValue()) + " / str: "
                            + (new String(value.getValue())) + " from: "
                            + originator.getAddress().getAddress().getHostAddress() + "\n");
                }
            }

            @Override
            public void valueWritten(DHTPluginContact target, DHTPluginValue value) {
                synchronized (async_debug) {
                    async_debug.append((new Date()) + ": valueWritten at: "
                            + target.getAddress().getAddress().getHostAddress() + "\n");
                }
            }

            @Override
            public void starts(byte[] key) {
                // TODO Auto-generated method stub

            }
        };

        System.out.println("debug, which: " + which);
        if (which.equals("remove all largest-file-audio swarms")) {

            System.out.println("trying to remove all largest audio...");

            StringBuilder sb = new StringBuilder();

            for (org.gudy.azureus2.core3.download.DownloadManager dm : (List<org.gudy.azureus2.core3.download.DownloadManager>) AzureusCoreImpl
                    .getSingleton().getGlobalManager().getDownloadManagers()) {
                if (dm.getTorrent() == null) {
                    sb.append("*** NULL torrent: " + dm.getDisplayName() + " / state: "
                            + dm.getState() + " / " + dm.getErrorDetails() + "\n");
                } else {
                    File out = dm.getSaveLocation();
                    UpdatingFileTree tree = new UpdatingFileTree(out,
                            new UpdatingFileTreeListener() {
                                @Override
                                public void broadcastChange(UpdatingFileTree path, boolean isDelete) {
                                }
                            });
                    if (MagicDecider.checkAudioDirectory(tree)) {
                        sb.append("removing audio torrent: " + dm.getDisplayName() + " / "
                                + out.getAbsolutePath() + "\n");
                        try {
                            AzureusCoreImpl.getSingleton().getGlobalManager()
                                    .removeDownloadManager(dm, false, false);
                        } catch (AzureusCoreException e) {
                            e.printStackTrace();
                        } catch (GlobalManagerDownloadRemovalVetoException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            return sb.toString();

        } else if (which.equals("friend ids")) {
            StringBuilder sb = new StringBuilder();
            FriendInfoLite[] friends = coreInterface.getF2FInterface().getFriends();
            for (FriendInfoLite f : friends) {
                sb.append(f.getName() + " G: " + f.getGroup() + " S: " + f.getSource() + " conn: "
                        + f.getConnectionId() + " pub: " + f.getPublicKey() + "\n");
            }
            return sb.toString();
        } else if (which.equals("friend logs")) {
            StringBuilder sb = new StringBuilder();
            FriendInfoLite[] friends = coreInterface.getF2FInterface().getFriends();
            for (FriendInfoLite f : friends) {
                sb.append(f.getName() + "\n" + f.getConnectionLog() + "\n");
            }
            return sb.toString();
        } else if (which.equals("DL managers")) {
            StringBuilder sb = new StringBuilder();
            for (org.gudy.azureus2.core3.download.DownloadManager dm : (List<org.gudy.azureus2.core3.download.DownloadManager>) AzureusCoreImpl
                    .getSingleton().getGlobalManager().getDownloadManagers()) {
                if (dm.getTorrent() == null) {
                    sb.append("*** NULL torrent: " + dm.getDisplayName() + " / state: "
                            + dm.getState() + " / " + dm.getErrorDetails() + "\n");
                } else {
                    TorrentInfo info = null;
                    try {
                        info = TorrentInfoFactory.create(coreInterface.getF2FInterface(),
                                coreInterface.getDownload(OneSwarmHashUtils.createOneSwarmHash(dm
                                        .getTorrent().getHash())), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        int prog = info == null ? -1 : info.getProgress();
                        sb.append(dm.getDisplayName() + " "
                                + ByteFormatter.encodeString(dm.getTorrent().getHash()) + " 32: "
                                + Base32.encode(dm.getTorrent().getHash()) + " 64: "
                                + (new String(Base64.encode(dm.getTorrent().getHash())))
                                + " state: " + dm.getState() + " size: " + dm.getSize() + " loc: "
                                + dm.getSaveLocation() + " tor: " + dm.getTorrentFileName()
                                + " prog: " + prog + "\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return sb.toString();
        } else if (which.equals("friend files")) {
            StringBuilder sb = new StringBuilder();
            for (FriendInfoLite f : coreInterface.getF2FInterface().getFriends(true, true)) {
                try {
                    sb.append(f.getName() + " (" + f.getStatus() + ")\n");
                    List<FileListLite> fileList = coreInterface.getF2FInterface().getFileList(
                            f.getConnectionId(), "", 0, Integer.MAX_VALUE, 3600000);
                    for (FileListLite file : fileList) {
                        sb.append("\t" + file.getCollectionName() + " "
                                + file.getTotalFilesInGroup() + " file(s)\n");
                    }
                    sb.append("\n");
                } catch (Exception e) {
                    System.err.println("exception for friend: " + f.getName());
                    e.printStackTrace();
                }
            }
            return sb.toString();
        } else if (which.equals("dht")) {
            StringBuilder sb = new StringBuilder();
            byte[] mykey = Base64.decode(coreInterface.getF2FInterface().getMyPublicKey());
            for (FriendInfoLite f : coreInterface.getF2FInterface().getFriends(true, true)) {
                byte[] theirkey = Base64.decode(f.getPublicKey());
                // from FriendConnector.java
                byte[] dhtkey = new byte[theirkey.length + mykey.length];
                System.arraycopy(mykey, 0, dhtkey, 0, mykey.length);
                System.arraycopy(theirkey, 0, dhtkey, mykey.length, theirkey.length);
                sb.append(f.getName() + " key: " + Base64.encode(dhtkey) + "\n\n");
            }

            DHTPlugin dht_plug = (DHTPlugin) AzureusCoreImpl.getSingleton().getPluginManager()
                    .getPluginInterfaceByClass(DHTPlugin.class).getPlugin();
            sb.append("local addr: " + dht_plug.getLocalAddress().getName() + "\n");
            sb.append("dht count: " + dht_plug.getDHTs().length + "\n");
            int i = 0;
            for (DHT dht : dht_plug.getDHTs()) {
                DHTDB db = dht.getDataBase();
                sb.append("dht " + i + " keys: " + db.getStats().getKeyCount() + "\n");
                Iterator<HashWrapper> itr = db.getKeys();
                while (itr.hasNext()) {
                    HashWrapper key = itr.next();
                    DHTDBValue value = db.get(key);
                    if (value != null) {
                        sb.append("\t" + Base64.encode(key.getBytes()) + " -> "
                                + Base64.encode(value.getValue()) + " @ "
                                + (new Date(value.getCreationTime())) + " local?: "
                                + Boolean.toString(value.isLocal()) + " flags: " + value.getFlags()
                                + "\n");
                    } else {
                        sb.append("\t" + Base64.encode(key.getBytes()) + " -> (null)\n");
                    }
                }
                sb.append("\n");

                i++;
            }

            return sb.toString();
        } else if (which.equals("refresh_community_servers")) {
            CommunityServerManager.get().refreshAll();
            return CommunityServerManager.get().debug();
        } else if (which.equals("check_async_output")) {
            synchronized (async_debug) {
                return async_debug.toString();
            }
        } else if (which.startsWith("dht_lookup")) {
            String keyBase64 = which.split("\\s+")[1];
            byte[] key = Base64.decode(keyBase64);
            DHTPlugin dht_plug = (DHTPlugin) AzureusCoreImpl.getSingleton().getPluginManager()
                    .getPluginInterfaceByClass(DHTPlugin.class).getPlugin();
            synchronized (async_debug) {
                async_debug.setLength(0);
                async_debug.append((new Date()) + ": starting lookup\n");
            }
            dht_plug.get(key, "debug_lookup", (byte) 0, 20, 120 * 1000, true, true, dht_op_listener);
            return "get " + Base64.encode(key);
        } else if (which.startsWith("param_set")) {
            try {
                String[] toks = which.split("\\s+");
                String param = toks[1], value = toks[2];
                COConfigurationManager.setParameter(param, value);
                return "set";
            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }
        } else if (which.startsWith("dht_put")) {
            StringBuilder sb = new StringBuilder();
            byte[] key = Base64.decode(which.split("\\s+")[1]);
            byte[] val = which.split("\\s+")[2].getBytes();
            sb.append(Base64.encode(key) + " -> " + Base64.encode(val));

            DHTPlugin dht_plug = (DHTPlugin) AzureusCoreImpl.getSingleton().getPluginManager()
                    .getPluginInterfaceByClass(DHTPlugin.class).getPlugin();
            synchronized (async_debug) {
                async_debug.setLength(0);
                async_debug.append((new Date()) + ": starting put\n");
            }
            dht_plug.put(key, "debug_put", val, (byte) 0, dht_op_listener);

            return sb.toString();
        } else if (which.equals("reshare_with_all_friends")) {

            for (org.gudy.azureus2.core3.download.DownloadManager dm : (List<org.gudy.azureus2.core3.download.DownloadManager>) AzureusCoreImpl
                    .getSingleton().getGlobalManager().getDownloadManagers()) {
                try {
                    ArrayList<GroupBean> g = new ArrayList<GroupBean>();
                    g.add(GroupBean.ALL_FRIENDS);
                    PermissionsDAO.get().setGroupsForHash(
                            ByteFormatter.encodeString(dm.getTorrent().getHash()), g, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return "done";

        } else if (which.equals("reshare unseen with all friends")) {
            try {
                fixAllPermissions();
            } catch (Exception e) {
                return e.toString();
            }
            return "done";
        } else if (which.equals("f2f debug")) {
            return coreInterface.getF2FInterface().getDebugInfo();
        } else if (which.equals("delete-chatdb")) {
            ChatDAO.get().dropTables();
            return "dropped 'em";
        } else if (which.equals("dump-messages")) {
            return ChatDAO.get().dump_table("messages");
        } else if (which.equals("disconnect all friends")) {
            coreInterface.getF2FInterface().stopTransfers();
            coreInterface.getF2FInterface().restartTransfers();
            return new Date() + ": disconnected all friends";
        } else if (which.equals("force connect to all friends")) {
            StringBuilder b = new StringBuilder();
            FriendInfoLite[] friends = coreInterface.getF2FInterface().getFriends(true, false);
            b.append("connecting to: \n");
            for (FriendInfoLite f : friends) {
                if (f.getStatus() != Friend.STATUS_ONLINE) {
                    coreInterface.getF2FInterface().connectToFriend(f.getPublicKey());
                    b.append(f.getName() + "\n");
                }
            }
            return b.toString();
        } else if (which.equals("error_dlog")) {
            BackendErrorLog.get().logString("error log test.", true);
        } else if (which.equals("id3")) {
            StringBuilder sb = new StringBuilder();
            int considered = 0, nostate = 0, with = 0;
            for (org.gudy.azureus2.core3.download.DownloadManager dm : (List<org.gudy.azureus2.core3.download.DownloadManager>) AzureusCoreImpl
                    .getSingleton().getGlobalManager().getDownloadManagers()) {
                considered++;
                if (dm.getDownloadState() == null) {
                    nostate++;
                    continue;
                }
                String album = dm.getDownloadState().getAttribute(
                        FileCollection.ONESWARM_ALBUM_ATTRIBUTE);
                String artist = dm.getDownloadState().getAttribute(
                        FileCollection.ONESWARM_ARTIST_ATTRIBUTE);
                if (artist != null || album != null) {
                    with++;
                    try {
                        sb.append(dm.getDisplayName() + " artist: " + artist + " album: " + album
                                + " " + ByteFormatter.encodeString(dm.getTorrent().getHash())
                                + "\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            sb.append("considred: " + considered + " with: " + with + " nostate:" + nostate);
            return sb.toString();
        } else if (which.equals("bind_audio")) {
            return MagicDirectoryManager.bind_audio_scan();
        } else if (which.equals("autotag_music")) {
            MagicDirectoryManager.autotag_existing_audio();
            return "done";
        } else if (which.equals("Remote Access")) {
            HashMap<String, Integer> counts = new HashMap<String, Integer>();
            HashMap<String, Long> speeds = new HashMap<String, Long>();
            RemoteAccessForward remoteAccessForward = coreInterface.getRemoteAccessForward();

            for (Map<String, String> map : remoteAccessForward.getRemoteAccessStats()) {
                String ip = map.get("remote_ip");
                if (!counts.containsKey(ip)) {
                    counts.put(ip, 0);
                    speeds.put(ip, 0l);
                }
                counts.put(ip, counts.get(ip) + 1);
                speeds.put(ip, counts.get(ip) + Long.parseLong(map.get("upload_rate")));
            }
            StringBuilder b = new StringBuilder();
            b.append("Summary:\n");
            for (String ip : counts.keySet()) {
                b.append(ip + " " + " num=" + counts.get(ip) + " speed=" + speeds.get(ip) + "\n");
            }
            b.append("\nDetails:\n");
            for (Map<String, String> map : remoteAccessForward.getRemoteAccessStats()) {
                b.append(map.get("remote_ip") + ": age=" + map.get("age") + " uploaded="
                        + map.get("total_uploaded") + " down=" + map.get("total_downloaded")
                        + " lan=" + map.get("lan_local") + " idle_in=" + map.get("idle_in")
                        + " idle_out=" + map.get("idle_out") + "\n");
            }
            return b.toString();
        } else if (which.equals("ffmpeg")) {
            FFMpegException e = FFMpegWrapper.getLastException();
            if (e != null) {
                return e.toString();
            } else {
                return "no ffmpeg expections found";
            }
        } else if (which.equals("searches")) {
            return coreInterface.getF2FInterface().getSearchDebugLog();
        } else if (which.equals("locks")) {
            return coreInterface.getF2FInterface().getLockDebug();
        } else if (which.equals("queue lengths")) {
            return coreInterface.getF2FInterface().getForwardQueueLengthDebug();
        } else if (which.equals("rpc profiling")) {
            ArrayList<RpcProfiling> entries = new ArrayList<RpcProfiling>();
            synchronized (rpcProfilingMap) {
                entries.addAll(rpcProfilingMap.values());
            }
            Collections.sort(entries);
            StringBuilder b = new StringBuilder();
            for (RpcProfiling r : entries) {
                b.append(r + "\n");
            }
            return b.toString();
        } else if (which.equals("backendtask")) {
            final int taskID = BackendTaskManager.get().createTask("test",
                    new CancellationListener() {
                        @Override
                        public void cancelled(int inID) {
                        }
                    });
            (new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (Exception e) {
                    }
                    BackendTaskManager.get().cancelTask(taskID);
                }
            }).start();
            return "created";
        } else if (which.equals("threads")) {
            StringBuilder out = new StringBuilder();

            Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
            for (Thread t : traces.keySet()) {
                out.append(t.getName() + " (" + t.getId() + ")\n");
                for (StackTraceElement e : traces.get(t)) {
                    out.append("\t" + e.getMethodName() + " (" + e.getFileName() + ":"
                            + e.getLineNumber() + ")\n");
                }
                out.append("\n");
            }

            return out.toString();
        } else if (which.equals("reload_logging")) {

            try {
                loadLogger(true);
            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }

            return "reloaded";
        } else if (which.equals("republish_location")) {
            final OSF2FMain f2fMain = OSF2FMain.getSingelton();
            return f2fMain.getDHTConnector().forceRepublish() + "";
        }

        return "don't know what to do with '" + which + "'";
    }

    private void fixAllPermissions() throws IOException {
        for (org.gudy.azureus2.core3.download.DownloadManager dm : (List<org.gudy.azureus2.core3.download.DownloadManager>) AzureusCoreImpl
                .getSingleton().getGlobalManager().getDownloadManagers()) {
            reshareUnseenWithAll(dm);
        }
    }

    private void reshareUnseenWithAll(org.gudy.azureus2.core3.download.DownloadManager dm)
            throws IOException {

        if (dm == null) {
            throw new IOException("Couldn't locate swarm from hash.");
        }

        try {
            List<GroupBean> existing = PermissionsDAO.get().getGroupsForHash(
                    ByteFormatter.encodeString(dm.getTorrent().getHash()));
            if (existing != null) {
                if (existing.size() == 0) {
                    System.out.println("sharing with all friends: " + dm.getDisplayName());
                    ArrayList<GroupBean> def = new ArrayList<GroupBean>();
                    def.add(GroupBean.ALL_FRIENDS);
                    PermissionsDAO.get().setGroupsForHash(
                            ByteFormatter.encodeString(dm.getTorrent().getHash()), def, false);
                } else {
                    System.out.println(dm.getDisplayName() + " has " + existing.size());
                    for (GroupBean b : existing) {
                        System.out.println(b.getGroupName());
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException(e.toString());
        }
    }

    @Override
    public String getRemoteAccessUserName(String session) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("bad session cookie");
        }

        return RemoteAccessConfig.getRemoteAccessUserName();
    }

    @Override
    public String saveRemoteAccessCredentials(String session, String username, String password) {

        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("bad session cookie");
        }

        if (remoteAccess) {
            throw new RuntimeException("call not allowed over remote access");
        }
        try {
            RemoteAccessConfig.saveRemoteAccessCredentials(username, password);
            /*
             * now restart the forwarding
             */
            boolean oldValue = getBooleanParameterValue(session,
                    OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY);
            if (oldValue == true) {
                setBooleanParameterValue(session, OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY,
                        false);
                setBooleanParameterValue(session, OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY,
                        oldValue);
            }
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return e.getMessage();
        }
    }

    /**
     * returns the listening addresses, format, string[]
     * 
     * addresses[0]: external ip:port
     * 
     * addresses[1]: internal ip:port
     * 
     * @param session
     * @return addresses
     */
    @Override
    public String[] getListenAddresses(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("bad session: " + session);
            return new String[] { "bad session", "bad session" };
        }
        try {
            AZInstance myInstance = AzureusCoreImpl.getSingleton().getInstanceManager()
                    .getMyInstance();
            int port = COConfigurationManager.getIntParameter("TCP.Listen.Port");
            String external = myInstance.getExternalAddress().getHostAddress() + ":" + port;
            String internal = InetAddress.getLocalHost().getHostAddress() + ":" + port;

            if (internal.startsWith("127.")) {
                internal = external;
            }
            return new String[] { external, internal };
        } catch (UnknownHostException e) {
        }
        return new String[] { "", "" };
    }

    @Override
    public HashMap<String, Integer> getNewFriendsCountsFromAutoCheck(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("bad session: " + session);
            return new HashMap<String, Integer>();
        }

        /**
         * Add in the entries from community servers
         */
        HashMap<String, Integer> out = (HashMap<String, Integer>) coreInterface.getF2FInterface()
                .getNewFriendsCountsFromAutoCheck();
        out.put(FriendsImportCommunityServer.FRIEND_NETWORK_COMMUNITY_NAME, CommunityServerManager
                .get().getUnmunchedCount());

        return out;
    }

    @Override
    public HashMap<String, String> getDeniedIncomingConnections(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("bad session: " + session);
            return new HashMap<String, String>();
        }
        return coreInterface.getF2FInterface().getDeniedIncomingConnections();
    }

    @Override
    public String getPlatform(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("failed session (getPlatform): " + session);
            return null;
        }
        if (Constants.isOSX) {
            return "osx";
        }
        if (Constants.isFreeBSD) {
            return "bsd";
        }
        if (Constants.isLinux) {
            return "linux";
        }
        if (Constants.isSolaris) {
            return "solaris";
        }
        if (Constants.isWindows) {
            return "windows";
        }
        return "unknown";
    }

    // public void deleteFriend(String session, FriendInfoLite friend) {
    // if (this.passedSessionIDCheck(session) == false) {
    // throw new RuntimeException("Bad cookie");
    // }
    // coreInterface.getF2FInterface().deleteFriend(friend);
    // }

    @Override
    public void deleteFriends(String session, FriendInfoLite[] friends) {
        try {
            if (this.passedSessionIDCheck(session) == false) {
                throw new IOException("bad cookie");
            }
            for (FriendInfoLite f : friends) {
                coreInterface.getF2FInterface().deleteFriend(f);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void addToIgnoreRequestList(String session, FriendInfoLite friend) {
        if (this.passedSessionIDCheck(session) == false) {
            throw new RuntimeException("Bad cookie");
        }
        coreInterface.getF2FInterface().addToIgnoreRequestList(friend);
    }

    @Override
    public String getGtalkStatus(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            throw new RuntimeException("Bad cookie");
        }
        return coreInterface.getF2FInterface().getGtalkStatus();
    }

    @Override
    public FileTree getAllTags(String session) {
        try {
            return StatelessSwarmFilter.getTagsFromSwarms(AzureusCoreImpl.getSingleton()
                    .getGlobalManager().getDownloadManagers());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public FileTree getTags(String session, String inOneSwarmHash) throws OneSwarmException {
        if (this.passedSessionIDCheck(session) == false) {
            throw new OneSwarmException("bad cookie");
        }

        org.gudy.azureus2.core3.download.DownloadManager dm = AzureusCoreImpl.getSingleton()
                .getGlobalManager().getDownloadManager(getInfohashForTorrentID(inOneSwarmHash));
        if (dm == null) {
            throw new OneSwarmException("Tried to obtain tags for a nonexistent swarm: "
                    + inOneSwarmHash);
        }

        return StatelessSwarmFilter.getTagsFromSwarms(Arrays
                .asList(new org.gudy.azureus2.core3.download.DownloadManager[] { dm }));

        // String [] tags =
        // dm.getDownloadState().getListAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE);
        // if( tags == null ) {
        // return new String[]{};
        // } else {
        // return tags;
        // }
    }

    @Override
    public void setTags(String session, String inOneSwarmHash, String[] path) {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            org.gudy.azureus2.core3.download.DownloadManager dm = AzureusCoreImpl.getSingleton()
                    .getGlobalManager().getDownloadManager(getInfohashForTorrentID(inOneSwarmHash));

            if (path == null) {
                dm.getDownloadState()
                        .setListAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE, null);
            } else {
                dm.getDownloadState()
                        .setListAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE, path);
                for (String p : path) {
                    System.out.println("adding path: " + p + " to " + dm.getDisplayName());
                }
            }

            PermissionsDAO.get().refreshFileLists();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public FriendInfoLite getSelf(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            throw new RuntimeException("Bad cookie");
        }

        FriendInfoLite f = new FriendInfoLite();
        f.setName(getComputerName(session));
        f.setPublicKey(getMyPublicKey(session));
        AZInstance myInstance = AzureusCoreImpl.getSingleton().getInstanceManager().getMyInstance();
        try {
            String external = myInstance.getExternalAddress().getHostAddress();
            String internal = InetAddress.getLocalHost().getHostAddress();
            if (external.equals(internal) || internal.startsWith("127.")) {
                f.setLastConnectIp(external);
            } else {
                f.setLastConnectIp(external + ", " + internal);
            }
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        OverallStats stats = StatsFactory.getStats();
        f.setDownloadedTotal(stats.getDownloadedBytes());
        f.setUploadedTotal(stats.getUploadedBytes());

        return f;
    }

    @Override
    public HashMap<String, String[]> getUsersWithMessages(String session) {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            HashMap<String, String[]> keyToNickAndUnread = new HashMap<String, String[]>();

            HashMap<String, Integer> unread_counts = ChatDAO.get().getUnreadMessageCounts();

            for (String base64Key : ChatDAO.get().getUsersWithMessages()) {
                Friend friend = coreInterface.getF2FInterface().getFriend(base64Key);
                int unread = 0;
                if (unread_counts.containsKey(base64Key)) {
                    unread = unread_counts.get(base64Key);
                }
                if (friend != null) {
                    keyToNickAndUnread.put(base64Key,
                            new String[] { friend.getNick(), Integer.toString(unread) });
                } else {
                    keyToNickAndUnread.put(base64Key,
                            new String[] { "Unknown user", Integer.toString(unread) });
                }
            }

            return keyToNickAndUnread;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public HashMap<String, Integer> getUnreadMessageCounts(String session) {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }
            return ChatDAO.get().getUnreadMessageCounts();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public SerialChatMessage[] getMessagesForUser(String session, String base64PublicKey,
            boolean include_read, int limit) {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            List<Chat> messages = ChatDAO.get().getMessagesForUser(base64PublicKey, include_read,
                    limit);
            SerialChatMessage[] out = new SerialChatMessage[messages.size()];
            for (int i = 0; i < messages.size(); i++) {
                SerialChatMessage neu = new SerialChatMessage();
                Chat c = messages.get(i);

                neu.setSent(c.isSent());
                neu.setUid(c.getUID());
                neu.setMessage(c.getMessage());
                neu.setNickname(c.getNick());
                neu.setTimestamp(c.getTimestamp());
                neu.setUnread(c.isUnread());
                neu.setOutgoing(c.isOutgoing());

                out[i] = neu;
            }

            return out;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean sendChatMessage(String session, String base64PublicKey, SerialChatMessage message)
            throws OneSwarmException {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            Friend friend = coreInterface.getF2FInterface().getFriend(base64PublicKey);
            if (friend == null) {
                throw new UnknownUserException();
            }

            boolean online = true;
            if (friend.getConnectionId() == Friend.NOT_CONNECTED_CONNECTION_ID) {
                online = false;
            }

            Chat converted = new Chat(-1, getComputerName(session), System.currentTimeMillis(),
                    message.getMessage(), true, true, online);
            ChatDAO.get().recordOutgoing(converted, base64PublicKey);

            if (online) {
                coreInterface.getF2FInterface().sendChatMessage(friend, message.getMessage());
            }

            return online;

        } catch (OneSwarmException e) {
            throw e;
        } catch (Exception e) {
            throw new OneSwarmException(getStackTraceAsString(e));
        }
    }

    @Override
    public int clearChatLog(String session, String base64PublicKey) {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new Exception("bad cookie");
            }

            return ChatDAO.get().deleteUsersMessages(base64PublicKey);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void updateRemoteAccessIpFilter(String session, String selectedFilterType,
            String filterString) throws OneSwarmException {
        if (!this.passedSessionIDCheck(session)) {
            throw new OneSwarmException("bad cookie");
        }
        if (OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_RANGE.equals(selectedFilterType)) {
            RemoteAccessForward.createIpFilter(filterString);
            setStringParameterValue(session, OneSwarmConstants.REMOTE_ACCESS_LIMIT_IPS_KEY,
                    filterString);
        }
        setStringParameterValue(session, OneSwarmConstants.REMOTE_ACCESS_LIMIT_TYPE_KEY,
                selectedFilterType);
        /*
         * now restart the forwarding
         */
        boolean oldValue = getBooleanParameterValue(session,
                OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY);
        if (oldValue == true) {
            setBooleanParameterValue(session, OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY, false);
            setBooleanParameterValue(session, OneSwarmConstants.REMOTE_ACCESS_PROPERTIES_KEY,
                    oldValue);
        }
    }

    @Override
    public String getDebugMessageLog(String session, String friendPublicKey) {
        if (!this.passedSessionIDCheck(session)) {
            throw new RuntimeException("bad cookie");
        }

        return coreInterface.getF2FInterface().getDebugMessageLog(friendPublicKey);
    }

    @Override
    public ArrayList<BackendErrorReport> getBackendErrors(String session) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: getBackenedErrors");
            return null;
        }
        List<BackendErrorLog.ErrorReport> reports = BackendErrorLog.get().getReports();
        ArrayList<BackendErrorReport> serialized = new ArrayList<BackendErrorReport>();
        for (BackendErrorLog.ErrorReport report : reports) {
            serialized.add(new BackendErrorReport(report.message, report.show_report_text));
        }
        return serialized;
    }

    @Override
    public String[] getBase64HashesForOneSwarmHashes(String session, String[] inOneSwarmHashes) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad session: getBase64HashesForOneSwarmHashes / " + session);
            return null;
        }
        try {
            String[] out = new String[inOneSwarmHashes.length];
            for (int i = 0; i < out.length; i++) {
                out[i] = new String(Base64.encode(OneSwarmHashUtils
                        .bytesFromOneSwarmHash(inOneSwarmHashes[i])));
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String[] getBase64HashesForBase32s(String session, String[] inBase32s)
            throws OneSwarmException {
        try {
            if (!this.passedSessionIDCheck(session)) {
                throw new IOException("Bad cookie");
            }

            if (inBase32s == null) {
                return null;
            }

            String[] out = new String[inBase32s.length];
            for (int i = 0; i < out.length; i++) {
                if (inBase32s[i] == null) {
                    logger.warning("Null base32 hash at position: " + i);
                    continue;
                }

                out[i] = new String(Base64.encode(Base32.decode(inBase32s[i])));
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            throw new OneSwarmException(getStackTraceAsString(e));
        }
    }

    @Override
    public FriendInvitationLite createInvitation(String session, String name,
            boolean canSeeFileList, long maxAge, SecurityLevel securityLevel) {
        if (this.passedSessionIDCheck(session) == false) {
            throw new RuntimeException("Bad cookie");
        }
        return FriendInvitationLiteFactory.createFriendInvitationLite(coreInterface
                .getF2FInterface().createInvitation(name, canSeeFileList, maxAge,
                        securityLevel.getLevel()));
    }

    @Override
    public void redeemInvitation(String session, FriendInvitationLite invitation, boolean testOnly)
            throws OneSwarmException {
        if (this.passedSessionIDCheck(session) == false) {
            throw new RuntimeException("Bad cookie");
        }
        try {
            FriendInvitation fi = FriendInvitationLiteFactory.createFriendInvitation(invitation);
            coreInterface.getF2FInterface().redeemInvitation(fi, testOnly);

        } catch (Exception e) {
            // System.err.println("error:" + e.getMessage());
            OneSwarmException osex = new OneSwarmException(e, true);
            // System.err.println("origi: " + osex.getMessage());
            throw osex;
        }
    }

    @Override
    public ArrayList<FriendInvitationLite> getSentFriendInvitations(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            throw new RuntimeException("Bad cookie");
        }
        ArrayList<FriendInvitationLite> il = new ArrayList<FriendInvitationLite>();
        ArrayList<FriendInvitation> i = (ArrayList<FriendInvitation>) coreInterface
                .getF2FInterface().getLocallyCreatedInvitations();
        for (FriendInvitation fi : i) {
            il.add(FriendInvitationLiteFactory.createFriendInvitationLite(fi));
        }

        return il;
    }

    @Override
    public ArrayList<FriendInvitationLite> getRedeemedFriendInvitations(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            throw new RuntimeException("Bad cookie");
        }
        ArrayList<FriendInvitationLite> il = new ArrayList<FriendInvitationLite>();
        ArrayList<FriendInvitation> i = (ArrayList<FriendInvitation>) coreInterface
                .getF2FInterface().getRedeemedInvitations();
        for (FriendInvitation fi : i) {
            il.add(FriendInvitationLiteFactory.createFriendInvitationLite(fi));
        }

        return il;
    }

    @Override
    public void updateFriendInvitations(String session, FriendInvitationLite invitation) {
        if (this.passedSessionIDCheck(session) == false) {
            throw new RuntimeException("Bad cookie");
        }
        try {
            coreInterface.getF2FInterface().updateInvitation(
                    FriendInvitationLiteFactory.createFriendInvitation(invitation));
        } catch (Exception e) {
            Debug.out("unable to update: " + invitation.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public void deleteFriendInvitations(String session, ArrayList<FriendInvitationLite> invitations) {
        if (this.passedSessionIDCheck(session) == false) {
            throw new RuntimeException("Bad cookie");
        }
        for (FriendInvitationLite invitation : invitations) {
            try {
                coreInterface.getF2FInterface().deleteInvitation(
                        FriendInvitationLiteFactory.createFriendInvitation(invitation));
            } catch (Exception e) {
                Debug.out("unable to delete invitation: " + invitation.getName() + ": "
                        + e.getMessage());
            }
        }
    }

    @Override
    public String copyTorrentInfoToMagnetLink(String session, String[] torrentIDs)
            throws OneSwarmException {
        try {
            if (this.passedSessionIDCheck(session) == false) {
                throw new IOException("Bad cookie");
            }

            if (torrentIDs == null) {
                throw new IOException("null torrentIDs");
            }

            if (torrentIDs.length == 0) {
                throw new IOException("zero-length torrentIDs");
            }

            String link = "oneswarm:?xt=urn:osih:"
                    + Base32.encode(OneSwarmHashUtils.bytesFromOneSwarmHash(torrentIDs[0]));

            if (GraphicsEnvironment.isHeadless() == true) {
                return link;
            }

            StringSelection transferString = new StringSelection(link);
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(transferString, transferString);

            return link;
        } catch (Exception e) {
            e.printStackTrace();
            throw new OneSwarmException(getStackTraceAsString(e));
        }
    }

    @Override
    public void refreshFileAssociations(String session) throws OneSwarmException {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("bad cookie: refreshFileAssociations()");
            return;
        }

        try {
            System.out.println("Refreshing file associations...");
            PlatformManagerFactory.getPlatformManager().registerApplication();
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
            throw new OneSwarmException(getStackTraceAsString(e));
        }
    }

    @Override
    public LocaleLite[] getLocales(String session) {
        if (!this.passedSessionIDCheck(session)) {
            throw new RuntimeException("bad cookie: getLocales()");
        }
        try {
            List<Locale> locales = TranslationTools.getLocales();
            LocaleLite[] localeLites = new LocaleLite[locales.size()];
            int pos = 0;
            for (Locale locale : locales) {
                localeLites[pos] = new LocaleLite(locale.getDisplayCountry(locale),
                        locale.getDisplayLanguage(locale), locale.toString());
                pos++;
            }
            return localeLites;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public HashMap<String, String> getFileInfo(String session, FileListLite file,
            boolean getFFmpegData) throws OneSwarmException {
        HashMap<String, String> v = new HashMap<String, String>();
        byte[] hash = Base64.decode(file.getCollectionId());
        Download d = coreInterface.getDownload(OneSwarmHashUtils.createOneSwarmHash(hash));
        if (d == null) {
            // System.out.println("download not found: " +
            // file.getCollectionId());
            return v;
        }
        DiskManagerFileInfo[] diskManagerFileInfo = d.getDiskManagerFileInfo();
        DiskManagerFileInfo diskFile = null;

        // System.out.println("file=" + file.getFileName());
        for (DiskManagerFileInfo dmf : diskManagerFileInfo) {
            String name = dmf.getFile().getName();
            // System.out.println("checking:" + name);
            if (name.equals(file.getFileName())) {
                diskFile = dmf;
                // System.out.println("match");
            }
        }
        if (diskFile == null) {
            // System.out.println("diskfile not found");
            return v;
        }
        v.put("  File", diskFile.getFile().getAbsolutePath());
        if (file.getSha1Hash() != null) {
            v.put("  hash_sha1", file.getSha1Hash());
        }
        String base64Ed2kHash = file.getEd2kHash();
        if (base64Ed2kHash != null) {
            v.put("  hash_ed2k", ShareManagerTools.base16Encode(Base64.decode(base64Ed2kHash)));
        }
        org.gudy.azureus2.core3.download.DownloadManager real_dl = AzureusCoreImpl.getSingleton()
                .getGlobalManager().getDownloadManager(new HashWrapper(hash));

        if (real_dl != null && real_dl.getDownloadState() != null) {
            String hashSource = real_dl.getDownloadState().getAttribute(
                    Sha1HashManager.OS_HASHES_ADDED);
            if (hashSource != null) {
                v.put("  hash_src", hashSource);
            }

            String album = real_dl.getDownloadState().getAttribute(
                    FileCollection.ONESWARM_ALBUM_ATTRIBUTE);
            if (album != null) {
                v.put("  id3 album", album);
            }
            String artist = real_dl.getDownloadState().getAttribute(
                    FileCollection.ONESWARM_ARTIST_ATTRIBUTE);
            if (artist != null) {
                v.put("  id3 artist", artist);
            }

            for (String tagpath : real_dl.getDownloadState().getListAttribute(
                    FileCollection.ONESWARM_TAGS_ATTRIBUTE)) {
                v.put("  tag", tagpath);
            }
        }
        if (getFFmpegData) {
            try {
                v.putAll(FFMpegAsyncOperationManager
                        .getInstance()
                        .getMovieInfo(d.getTorrent().getHash(), diskFile.getFile(), 2,
                                TimeUnit.SECONDS).getAsUiMap());
            } catch (DataNotAvailableException e) {
                // this is ok, next time we call the method we will get this
                // info included, no blocking operations allowed on xml rpc
                // threads...
            }
        }

        return v;
    }

    @Override
    public void applyDefaultSettings(String session) {
        if (!this.passedSessionIDCheck(session)) {
            System.err.println("Bad session: " + session);
            return;
        }

        /**
         * Add default UW community server and immediately refresh
         */
        ArrayList<String> existingCommunityServers = getStringListParameterValue(session,
                "oneswarm.community.servers");
        boolean hasIt = false;
        for (String entry : existingCommunityServers) {
            if (entry.equals(CommunityServerAddPanel.DEFAULT_COMMUNITY_SERVER)) {
                hasIt = true;
            }
        }
        if (hasIt == false) {
            CommunityRecord rec = new CommunityRecord(Arrays.asList(new String[] {
                    CommunityServerAddPanel.DEFAULT_COMMUNITY_SERVER, "", "", "Community contacts",
                    "true;false;false;false;50" }), 0);

            existingCommunityServers.addAll(Arrays.asList(rec.toTokens()));

            setStringListParameterValue(session, "oneswarm.community.servers",
                    existingCommunityServers);
            CommunityServerManager.get().refreshAll();
        }

        /**
         * Now do upload speed test...
         */
        performSpeedCheck(session, 0.85);
    }

    @Override
    public int getNumberFriendsCount(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("Bad session: " + session);
            return 0;
        }

        return coreInterface.getF2FInterface().getFriends().length;
    }

    @Override
    public int getNumberOnlineFriends(String session) {
        if (this.passedSessionIDCheck(session) == false) {
            System.err.println("Bad session: " + session);
            return 0;
        }

        return coreInterface.getF2FInterface().getFriends(false, false).length;
    }

    @Override
    public BackendTask performSpeedCheck(final String session, final double setWithFraction) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("invalid cookie");
        }

        final int speedCheckId = coreInterface.getF2FInterface().performSpeedCheck();
        final int taskId = BackendTaskManager.get().createTask("Speed check",
                new CancellationListener() {
                    @Override
                    public void cancelled(int inID) {
                        coreInterface.getF2FInterface().cancelSpeedCheck(speedCheckId);
                    }
                });

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean quit = false;
                    while (!quit) {
                        BackendTask task = BackendTaskManager.get().getTask(taskId);
                        HashMap<String, Double> result = coreInterface.getF2FInterface()
                                .getSpeedCheckResult(speedCheckId);
                        if (result == null || result.size() == 0) {
                            quit = true;
                            break;
                        }

                        SpeedTestResult r = new SpeedTestResult(result);
                        task.setResult(r);
                        task.setProgress(Math.round(100 * r.getProgress()) + "%");
                        Thread.sleep(100);

                        if (r.getProgress() == 1) {
                            quit = true;

                            if (setWithFraction != 0) {
                                double computedRate = setWithFraction * r.getEstimatedUploadRate()
                                        / 1024.0;
                                int outRate = (int) Math.round(Math.max(computedRate, 10));
                                logger.info("Speed test result: " + r.getEstimatedUploadRate()
                                        + " set: " + outRate);
                                OneSwarmUIServiceImpl.this.setIntegerParameterValue(session,
                                        "Max Upload Speed KBs", outRate);
                            }
                        }
                    }

                    Thread.sleep(5 * 1000);
                    BackendTaskManager.get().removeTask(taskId);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        });
        t.setName("speed check backend task monitor");
        t.setDaemon(true);
        t.start();

        return BackendTaskManager.get().getTask(taskId);
    }

    @Override
    public BackendTask publishSwarms(String session, TorrentInfo[] infos, String[] previewPaths,
            String[] comments, String[] categories, CommunityRecord toServer) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("invalid cookie");
        }

        final PublishSwarmsThread pubThread = new PublishSwarmsThread(coreInterface, infos,
                previewPaths, comments, categories, toServer);

        final int taskId = BackendTaskManager.get().createTask(
                "Publishing " + infos.length + " swarms", new CancellationListener() {
                    @Override
                    public void cancelled(int inID) {
                        pubThread.cancel();
                    }
                });

        BackendTask task = BackendTaskManager.get().getTask(taskId);

        pubThread.associateTask(task);
        pubThread.start();

        return task;
    }

    @Override
    public ArrayList<String> getCategoriesForCommunityServer(String sessionID,
            CommunityRecord selected) {
        if (!passedSessionIDCheck(sessionID)) {
            System.err.println("invalid cookie");
            return null;
        }

        ArrayList<String> out = new ArrayList<String>();

        try {
            KeyPublishOp req = new KeyPublishOp(selected, false);
            ArrayList<String> cats = req.getCategories();
            return cats;

        } catch (Exception e) {
            logger.warning(e.toString());
            return null;
        }
    }

    @Override
    public void triggerNatCheck(String sessionID) {
        if (!passedSessionIDCheck(sessionID)) {
            System.err.println("invalid cookie");
            return;
        }
        coreInterface.getF2FInterface().triggerNATCheck();
    }

    @Override
    public HashMap<String, String> getNatCheckResult(String sessionID) {
        if (!passedSessionIDCheck(sessionID)) {
            System.err.println("invalid cookie");
            return null;
        }
        return coreInterface.getF2FInterface().getNatCheckResult();
    }

    @Override
    public void fixPermissions(String session, TorrentInfo torrent, boolean fixAll)
            throws OneSwarmException {
        if (!passedSessionIDCheck(session)) {
            System.err.println("invalid cookie");
            return;
        }
        try {
            if (fixAll) {
                fixAllPermissions();
            } else {
                reshareUnseenWithAll(AzureusCoreImpl
                        .getSingleton()
                        .getGlobalManager()
                        .getDownloadManager(
                                new HashWrapper(OneSwarmHashUtils.bytesFromOneSwarmHash(torrent
                                        .getTorrentID()))));
            }
        } catch (Exception e) {
            throw new OneSwarmException(e.toString());
        }
    }

    @Override
    public Boolean isStreamingDownload(String session, String torrentID) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("invalid cookie");
        }
        byte[] infohash = OneSwarmHashUtils.bytesFromOneSwarmHash(torrentID);
        if (infohash == null) {
            return false;
        }

        org.gudy.azureus2.core3.download.DownloadManager real_dl = AzureusCoreImpl.getSingleton()
                .getGlobalManager().getDownloadManager(new HashWrapper(infohash));

        if (real_dl == null) {
            return false;
        }
        if (real_dl.getDownloadState().hasAttribute(FileCollection.ONESWARM_STREAM_ATTRIBUTE)) {
            return real_dl.getDownloadState().getBooleanAttribute(
                    FileCollection.ONESWARM_STREAM_ATTRIBUTE);
        }
        return false;
    }

    @Override
    public void setStreamingDownload(String session, String torrentID, boolean streaming) {
        if (!passedSessionIDCheck(session)) {
            throw new RuntimeException("invalid cookie");
        }
        byte[] infohash = OneSwarmHashUtils.bytesFromOneSwarmHash(torrentID);
        if (infohash == null) {
            return;
        }

        org.gudy.azureus2.core3.download.DownloadManager real_dl = AzureusCoreImpl.getSingleton()
                .getGlobalManager().getDownloadManager(new HashWrapper(infohash));
        if (real_dl == null) {
            return;
        }
        real_dl.getDownloadState().setBooleanAttribute(FileCollection.ONESWARM_STREAM_ATTRIBUTE,
                streaming);
    }

    @Override
    public String getMultiTorrentSourceTemp(String session) {
        try {
            return Sha1DownloadManager.getMultiTorrentDownloadDir().getCanonicalPath();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public ArrayList<ClientServiceDTO> getClientServices() {
        List<ClientService> clientServices = ServiceSharingManager.getInstance()
                .getClientServices();
        ArrayList<ClientServiceDTO> serviceDTOs = new ArrayList<ClientServiceDTO>();
        for (ClientService clientService : clientServices) {
            serviceDTOs.add(clientService.toDTO());
        }
        return serviceDTOs;
    }

    @Override
    public ArrayList<SharedServiceDTO> getSharedServices() {
        List<SharedService> sharesServices = ServiceSharingManager.getInstance()
                .getSharedServices();
        ArrayList<SharedServiceDTO> serviceDTOs = new ArrayList<SharedServiceDTO>();
        for (SharedService sharedService : sharesServices) {
            serviceDTOs.add(sharedService.toDTO());
        }
        return serviceDTOs;
    }

    @Override
    public void saveClientServices(ArrayList<ClientServiceDTO> services) {
        ServiceSharingManager.getInstance().updateClients(services);
    }

    @Override
    public void saveSharedServices(ArrayList<SharedServiceDTO> services) throws OneSwarmException {
        try {
            ServiceSharingManager.getInstance().updateSharedServices(services);
        } catch (UnknownHostException e) {
            throw new OneSwarmException(e.getClass().getName() + "::" + e.getMessage());
        }
    }

}
