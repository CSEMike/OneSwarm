package edu.washington.cs.oneswarm.planetlab;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.apache.xerces.impl.dv.util.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.stats.transfer.StatsFactory;
import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.OSF2FMain;

public class CoordinatorHeartbeatThread extends Thread {

    private final String url;
    private final ExperimentConfigManager expConfig;

    private long lastSessionUL = 0, lastSessionDL = 0;
    private long lastSessionULTime = System.currentTimeMillis(), lastSessionDLTime = System
            .currentTimeMillis();

    private static Logger logger = Logger.getLogger(CoordinatorHeartbeatThread.class.getName());

    private boolean includeBuildInfo = true;

    public CoordinatorHeartbeatThread(ExperimentConfigManager experimentConfigManager, String url) {
        this.expConfig = experimentConfigManager;
        this.url = url;
        setDaemon(true);
        setName("Coordinator heartbeat: " + url);
    }

    @Override
    public void run() {
        final long started = System.currentTimeMillis();
        while (true) {
            try {
                AzureusCore core = AzureusCoreImpl.getSingleton();
                Map<String, String> formParams = new HashMap<String, String>();

                long sessionUL = core.getGlobalManager().getStats().getTotalDataBytesSent()
                        + core.getGlobalManager().getStats().getTotalProtocolBytesSent();
                long sessionDL = core.getGlobalManager().getStats().getTotalDataBytesReceived()
                        + core.getGlobalManager().getStats().getTotalProtocolBytesReceived();

                long minuteDL = 0, minuteUL = 0;

                try {
                    minuteDL = (sessionDL - lastSessionDL)
                            / ((System.currentTimeMillis() - lastSessionDLTime) / 1000);
                    minuteUL = (sessionUL - lastSessionUL)
                            / ((System.currentTimeMillis() - lastSessionULTime) / 1000);
                } catch (ArithmeticException e) {
                    logger.warning("Caught divide by zero (should be at startup)");
                }
                lastSessionDL = sessionDL;
                lastSessionUL = sessionUL;

                lastSessionDLTime = lastSessionULTime = System.currentTimeMillis();

                boolean connectorAvailable = false;
                try {
                    OSF2FMain f2fMain = OSF2FMain.getSingelton();
                    connectorAvailable = f2fMain.getDHTConnector() != null;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                formParams.put("ul", minuteDL + "");
                formParams.put("dl", minuteUL + "");
                formParams.put("mem", Runtime.getRuntime().totalMemory() + "");
                formParams.put("totfriends", expConfig.getCoreInterface().getF2FInterface()
                        .getFriends(true, true).length
                        + "");
                formParams.put("onlinefriends", expConfig.getCoreInterface().getF2FInterface()
                        .getFriends(false, false).length
                        + "");
                formParams.put("friendConnectorAvailable", connectorAvailable + "");
                formParams.put("started", started + "");
                formParams.put("sessionul", sessionUL + "");
                formParams.put("sessiondl", sessionDL + "");
                formParams.put("dls", core.getGlobalManager().getDownloadManagers().size() + "");
                formParams.put("totsent", StatsFactory.getStats().getUploadedBytes() + "");
                formParams.put("totrecv", StatsFactory.getStats().getDownloadedBytes() + "");
                formParams.put("totup", StatsFactory.getStats().getTotalUpTime() + "");
                formParams.put("key", expConfig.getCoreInterface().getF2FInterface()
                        .getMyPublicKey().replaceAll("\n", ""));

                String versionString = "Unknown";
                if (includeBuildInfo) {
                    try {
                        versionString = Constants.getOneSwarmAzureusModsVersion();
                        InputStream buildInfo = getClass().getClassLoader().getResourceAsStream(
                                "build.txt");
                        Properties info = new Properties();
                        info.load(buildInfo);
                        versionString = info.getProperty("build.number");
                    } catch (Exception e) {
                        logger.warning("Error loading build info: " + e.toString()
                                + " (This is expected in integration tests.)");
                        includeBuildInfo = false;
                    }
                }

                formParams.put("corevers", versionString);
                formParams.put("f2fvers",
                        Constants.getF2FVersion() != null ? Constants.getF2FVersion() : "built-in");
                formParams.put("gwtvers",
                        Constants.getWebUiVersion() != null ? Constants.getWebUiVersion()
                                : "built-in");

                formParams.put("clock", System.currentTimeMillis() + "");

                if (COConfigurationManager.getBooleanParameter("Proxy.Data.Enable")) {
                    formParams.put("dataProxy", "1");
                }

                StringBuffer dls = new StringBuffer();
                for (DownloadManager dm : (List<DownloadManager>) core.getGlobalManager()
                        .getDownloadManagers()) {
                    String hashStr = null;
                    try {
                        hashStr = Base64.encode(dm.getTorrent().getHash());
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    String completedStr = null;
                    try {
                        completedStr = dm.getStats().getCompleted() + "";
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                    dls.append(hashStr + " " + dm.getState() + " " + completedStr + "_");
                }
                formParams.put("dlsummary", dls.toString());

                synchronized (completion_times) {
                    StringBuffer completions = new StringBuffer();
                    for (DownloadManager done : completion_times.keySet()) {
                        completions.append(Base64.encode(done.getTorrent().getHash()) + " "
                                + completion_times.get(done) + " " + done.getTorrent().getSize()
                                + " " + bootstrap_times.get(done) + "_");
                    }
                    formParams.put("dlfinished", completions.toString());
                }

                logger.info("Registering with: " + url);
                HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection();
                conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                Iterator<String> params = formParams.keySet().iterator();
                while (params.hasNext()) {
                    String name = params.next();
                    String value = formParams.get(name);

                    if (value == null) {
                        logger.warning("Skipping encoding of null form parameter value: " + name);
                        continue;
                    }

                    out.append(URLEncoder.encode(name, "UTF-8") + "="
                            + URLEncoder.encode(value, "UTF-8"));
                    if (params.hasNext()) {
                        out.append("&");
                    }
                }
                out.flush();

                InputStream in = conn.getInputStream();
                byte[] dat = new byte[4 * 1024];
                int read = 0;
                ByteArrayOutputStream commands = new ByteArrayOutputStream();
                while ((read = in.read(dat, 0, dat.length)) > 0) {
                    commands.write(dat, 0, read);
                }
                (new CoordinatorExecutor(this, commands.toByteArray())).start();
                logger.info("read " + commands.size() + " bytes of response");

                completion_times.clear();
                bootstrap_times.clear();

            } catch (Exception e) {
                e.printStackTrace();
            }
            try {

                int secondsToWait = 60;
                try {
                    if (System.getProperty("oneswarm.test.coordinator.poll") != null) {
                        secondsToWait = Integer.parseInt(System
                                .getProperty("oneswarm.test.coordinator.poll"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Thread.sleep(secondsToWait * 1000); // 1 min
            } catch (Exception e) {
            }
        }
    }

    Map<DownloadManager, Long> completion_times = new ConcurrentHashMap<DownloadManager, Long>();
    Map<DownloadManager, Long> bootstrap_times = new ConcurrentHashMap<DownloadManager, Long>();

    public void downloadFinished(DownloadManager manager, long duration) {
        // synchronized(completion_times) {
        completion_times.put(manager, duration);
        // }
    }

    public void downloadBootstrapped(DownloadManager dm, long bootstrapInterval) {
        // synchronized(bootstrap_times) {
        bootstrap_times.put(dm, bootstrapInterval);
        // }
    }
}
