package edu.washington.cs.oneswarm.test.integration.oop;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import edu.washington.cs.oneswarm.f2f.ExperimentalHarnessManager;
import edu.washington.cs.oneswarm.f2f.OSF2FMain;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;

public class LocalOneSwarmCoordinatee extends Thread {
    private static Logger logger = Logger.getLogger(LocalOneSwarmCoordinatee.class.getName());
    private final String url;
    private final List<Entry<String, String>> pending = Collections
            .synchronizedList(new ArrayList<Entry<String, String>>());

    public LocalOneSwarmCoordinatee(String url) {
        this.url = url;

        setDaemon(true);
        setName("LocalOneSwarmCoordinatee to coordinator at " + url);
    }

    /** Adds a pending command. */
    public void addResponse(String key, String value) {
        pending.add(new FormEntry(key, value));
    }

    @Override
    public void run() {
        final long started = System.currentTimeMillis();
        while (true) {
            try {
                boolean connectorAvailable = false;
                try {
                    OSF2FMain f2fMain = OSF2FMain.getSingelton();
                    connectorAvailable = f2fMain.getDHTConnector() != null;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Map<String, String> formParams = new HashMap<String, String>();
                formParams.put("started", started + "");
                formParams.put("clock", System.currentTimeMillis() + "");
                CoreInterface ci = ExperimentalHarnessManager.get().getCoreInterface();
                if (ci != null) {
                    formParams.put("key", ci.getF2FInterface().getMyPublicKey()
                            .replaceAll("\n", ""));
                    formParams.put("onlinefriends",
                            ci.getF2FInterface().getFriends(false, false).length + "");
                }
                formParams.put("friendConnectorAvailable", connectorAvailable + "");
                synchronized (this.pending) {
                    for (Entry<String, String> entry : this.pending) {
                        formParams.put(entry.getKey(), entry.getValue());
                    }
                }

                logger.fine("Registering with: " + url);
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
                logger.fine("read " + commands.size() + " bytes of response");

                String response = commands.toString();
                ExperimentalHarnessManager.get().enqueue(response.split("\n"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Wait between checkins. Default to 1 minute.
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

                Thread.sleep(secondsToWait * 1000);
            } catch (Exception e) {
            }
        }
    }

    private class FormEntry implements Map.Entry<String, String> {
        private final String key;
        private String value;

        public FormEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String value) {
            String ret = this.value;
            this.value = value;
            return ret;
        }

    }
}
